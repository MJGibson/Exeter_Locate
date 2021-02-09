from flask import Flask, request, jsonify
import json
from pymongo import MongoClient


app = Flask(__name__)

magicNum = "aaz0p3DuHxgxqNOk40XA4csgjeEgJzC7AUEb40gTZXgtAM5TtpleDwdGkbXQICmKwCxuO2WXawQQiobWd3nggGH9plwgJHyERBF9"
dataBase = "phoneTest_home"

wifiCollection = "wifi"
combinedCollection = "combiColl"
gpsCollection = "gpsColl"

DEFAULT_GET_RESPONSE = (
    "<h1 style='color:blue'>RIBA2Reality Server Active!</h1>"
)

KEYS_REQUIRED_FOR_GPS = [
    "MAGIC_NUM",
    "UUID",
    "GPSTIME",
    "X",
    "Y",
    "ALTITUDE",
    "ACC",
]

KEYS_REQUIRED_FOR_WIFI = [
    "MAGIC_NUM",
    "UUID",
    "TIME",
    "MacAddressesJson",
    "signalStrengthsJson",
]

KEYS_REQUIRED_FOR_COMBINED = list(
    # get the unique elements across to two lists to avoid repetition
    set(KEYS_REQUIRED_FOR_GPS).union(KEYS_REQUIRED_FOR_WIFI)
)


def parse_request(request):
    # get the data out of the immutable dict object
    key_list = list(request.form.to_dict().keys())

    # if we received no data, end here
    if len(key_list) == 0:
        raise ValueError("Empty request")

    # check we've not just been sent garbage
    try:
        jsonData = json.loads(key_list[0])
        jsonData.keys()

    except AttributeError:
        raise ValueError("Bad request")

    return jsonData


def print_and_jsonify(msg):
    # converts msg to a string, print it and
    # return a flask request
    print(msg)
    return jsonify({"ERROR": str(msg)})


@app.route("/", methods=["GET", "POST"])
def gps():
    if request.method == "POST":
        # ---- Error checking

        # check we can parse the request
        try:
            jsonData = parse_request(request)
        except Exception as e:
            return print_and_jsonify(e)

        # check all the required fields are there
        for key in KEYS_REQUIRED_FOR_COMBINED:
            if key not in jsonData.keys():
                msg = f"Missing: {key:s}"
                return print_and_jsonify(msg)

        # check the magic number matches
        if jsonData["MAGIC_NUM"] != magicNum:
            return print_and_jsonify("magic number mismatch")

        # ---- connect to the db
        client = MongoClient("localhost", 27017)
        if "DATABASE" in jsonData.keys():
            db = client[jsonData["DATABASE"]]
        else:
            db = client[dataBase]

        # ---- post data to the GPS table
        collection = db[gpsCollection]
        collection.insert_one(
            {
                "UUID": jsonData["UUID"],
                "GPSTIME": jsonData["GPSTIME"],
                "x": float(jsonData["X"]),
                "y": float(jsonData["Y"]),
                "z": float(jsonData["ALTITUDE"]),
                "acc": float(jsonData["ACC"]),
            }
        )

        # ---- post data to the combined table

        # extract the mac addresses and their signat strengths
        MacAddressesJSON = json.loads(jsonData["MacAddressesJson"])
        signalStregthsJSON = json.loads(jsonData["signalStrengthsJson"])

        # check we've got some WiFi access points to post
        if len(MacAddressesJSON) == 0 or len(signalStregthsJSON) == 0:
            return "Server: GPS data stored but no WiFi access points included in post."

        else:
            # combine each record into a list to update the db in one go
            records = []
            for mac, strength in zip(MacAddressesJSON, signalStregthsJSON):
                records.append(
                    {
                        "UUID": jsonData["UUID"],
                        "Time": jsonData["TIME"],
                        "Macs": mac,
                        "level": int(strength),
                        "GPSTIME": jsonData["GPSTIME"],
                        "x": float(jsonData["X"]),
                        "y": float(jsonData["Y"]),
                        "z": float(jsonData["ALTITUDE"]),
                        "acc": float(jsonData["ACC"]),
                    }
                )

            # select the collection and post the data
            collection = db[combinedCollection]
            collection.insert_many(records)
            return "Server: Combined GPS and WiFi data stored successfully."

    return DEFAULT_GET_RESPONSE


@app.route("/wifi/", methods=["GET", "POST"])
def wifi():
    if request.method == "POST":
        # ---- Error checking

        # check we can parse the request
        try:
            jsonData = parse_request(request)
        except Exception as e:
            return print_and_jsonify(e)

        # check all the required fields are there
        for key in KEYS_REQUIRED_FOR_WIFI:
            if key not in jsonData.keys():
                msg = f"Missing: {key:s}"
                return print_and_jsonify(msg)

        # check the magic number matches
        if jsonData["MAGIC_NUM"] != magicNum:
            return print_and_jsonify("magic number mismatch")

        # ---- connect to the db
        client = MongoClient("localhost", 27017)
        if "DATABASE" in jsonData.keys():
            db = client[jsonData["DATABASE"]]
        else:
            db = client[dataBase]

        # ---- post data to the WIFI table

        # extract the mac addresses and their signal strengths
        MacAddressesJSON = json.loads(jsonData["MacAddressesJson"])
        signalStregthsJSON = json.loads(jsonData["signalStrengthsJson"])

        # check we've got some WiFi access points to post
        if len(MacAddressesJSON) == 0 or len(signalStregthsJSON) == 0:
            return "Server: No WiFi access points included in post."

        else:
            # combine each record into a list to update the db in one go
            records = []
            for mac, strength in zip(MacAddressesJSON, signalStregthsJSON):
                records.append(
                    {
                        "UUID": jsonData["UUID"],
                        "Time": jsonData["TIME"],
                        "Macs": mac,
                        "level": int(strength),
                    }
                )

            # select the collection and post the data
            collection = db[wifiCollection]
            collection.insert_many(records)
            return "Server: WiFi data stored successfully."

    return DEFAULT_GET_RESPONSE


if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True)
