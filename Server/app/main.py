from flask import Flask, request, jsonify
import json
from pymongo import MongoClient
import pandas as pd


app = Flask(__name__)

dataBase = "phoneTest_home"

wifiCollection = "wifi"
combinedCollection = "combiColl"
gpsCollection = "gpsColl"
magneticCollection = "mag"
accelCollection = "accel"
bleCollection = "ble"

DEFAULT_GET_RESPONSE = (
    "<h1 style='color:blue'>RIBA2Reality Server Active!</h1>"
)

KEYS_REQUIRED_FOR_GPS = [
    "UUID",
    "GPS_TIME",
    "X",
    "Y",
    "ALTITUDE",
    "ACC",
    "MESSAGE",
]

KEYS_REQUIRED_FOR_WIFI = [
    "UUID",
    "WIFI_TIME",
    "MacAddressesJson",
    "signalStrengthsJson",
    "MESSAGE",
]

KEYS_REQUIRED_FOR_BLE = [
    "UUID",
    "BLE_TIME",
    "MacAddressesJson",
    "signalStrengthsJson",
    "MESSAGE",
]

KEYS_REQUIRED_FOR_MAG = [
    "UUID",
    "MAG_TIME",
    "MAG_X",
    "MAG_Y",
    "MAG_Z",
    "MESSAGE",
]

KEYS_REQUIRED_FOR_ACCEL = [
    "UUID",
    "ACCEL_TIME",
    "ACCEL_X",
    "ACCEL_Y",
    "ACCEL_Z",
    "MESSAGE",
]

KEYS_REQUIRED_FOR_COMBINED = list(
    # get the unique elements across to two lists to avoid repetition
    set(KEYS_REQUIRED_FOR_GPS).union(
        KEYS_REQUIRED_FOR_WIFI,
        KEYS_REQUIRED_FOR_MAG,
        KEYS_REQUIRED_FOR_ACCEL,
        #KEYS_REQUIRED_FOR_BLE,
        ["MESSAGE", "matrix_R", "matrix_I"]
    )
)


PARAMETERS_FILE = 'parameters.dat'


#-------------------------------------------------------------------------------------
"""
    Adds lambda settings for GPS (and combined scans),Wi-fi, Posting, Bluetooth, accellerometer, 
    and magnetometer to the message passed as parameter. These lambda settings are the base times used 
    for poisson distribution.

    Parameters
    ----------
    message : str
        The input message
    

    Returns
    -------
    str
        The message with lambda settin appended


"""
def format_message(message):

    df = pd.read_csv(PARAMETERS_FILE)

    gpsLamda = df['gpsLambda'].values[0]
    wifiLamda = df['wifiLambda'].values[0]
    postLambda = df['postLambda'].values[0]
    bleLambda = df['bleLambda'].values[0]
    accelLambda = df['accelLambda'].values[0]
    magLambda = df['magLambda'].values[0]
    
    gpsDuration = df['gpsDuration'].values[0]
    gpsInterval = df['gpsInterval'].values[0]
    bleDuration = df['bleDuration'].values[0]
    

    return '{};{};{};{};{};{};{};{};{};{}'.format(
        message, 
        gpsLamda, 
        wifiLamda,
        postLambda,
        bleLambda,
        accelLambda,
        magLambda,
        gpsDuration,
        gpsInterval,
        bleDuration
        )

#-------------------------------------------------------------------------------------
def parse_request(request):
    # get the data out of the immutable dict object
    key_list = list(request.form.to_dict().keys())

    # if we received no data, end here
    if len(key_list) == 0:
        raise ValueError("Empty request")

    # check we've not just been sent garbage
    try:
        jsonData = request.form
        jsonData.keys()

    except AttributeError:
        raise ValueError("Bad request")

    return jsonData

#-------------------------------------------------------------------------------------
def print_and_jsonify(msg):
    # converts msg to a string, print it and
    # return a flask request
    print(msg)
    return jsonify({"ERROR": str(msg)})


#-------------------------------------------------------------------------------------
@app.route("/", methods=["GET", "POST"])
def combined():
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
        #if jsonData["MAGIC_NUM"] != magicNum:
        #    return print_and_jsonify("magic number mismatch")

        # ---- connect to the db
        client = MongoClient("mongodb://root:rootpassword@mongo", 27017)
        if "DATABASE" in jsonData.keys():
            db = client[jsonData["DATABASE"]]
        else:
            db = client[dataBase]

        # ---- post data to the GPS table
        # collection = db[gpsCollection]
        # collection.insert_one(
            # {
                # "UUID": jsonData["UUID"],
                # "GPS_TIME": jsonData["GPS_TIME"],
                # #"MESSAGE": jsonData["MESSAGE"],
                # "x": float(jsonData["X"]),
                # "y": float(jsonData["Y"]),
                # "z": float(jsonData["ALTITUDE"]),
                # "acc": float(jsonData["ACC"]),
            # }
        # )
        
        # # ---- post data to the MAG table

        # # combine each record into a list to update the db in one go
        # record = {
            # "UUID": jsonData["UUID"],
            # "MAG_TIME": jsonData["MAG_TIME"],
            # "MAG_x": float(jsonData["MAG_X"]),
            # "MAG_y": float(jsonData["MAG_Y"]),
            # "MAG_z": float(jsonData["MAG_Z"]),
            # }


        # # select the collection and post the data if there is any
        # collection = db[magneticCollection]
        
        # collection.insert(record)

        # ---- post data to the combined table

        # extract the mac addresses and their signat strengths
        MacAddressesJSON = json.loads(jsonData["MacAddressesJson"])
        signalStregthsJSON = json.loads(jsonData["signalStrengthsJson"])

        # check we've got some WiFi access points to post
        if len(MacAddressesJSON) == 0 or len(signalStregthsJSON) == 0:
        
            record = {
                        "UUID": jsonData["UUID"],
                        "MESSAGE": jsonData["MESSAGE"],
                        "WIFI_TIME": None,
                        "Macs": None,
                        "level": None,
                        "GPS_TIME": jsonData["GPS_TIME"],
                        "x": float(jsonData["X"]),
                        "y": float(jsonData["Y"]),
                        "z": float(jsonData["ALTITUDE"]),
                        "acc": float(jsonData["ACC"]),
                        "MAG_TIME": jsonData["MAG_TIME"],
                        "MAG_x": float(jsonData["MAG_X"]),
                        "MAG_y": float(jsonData["MAG_Y"]),
                        "MAG_z": float(jsonData["MAG_Z"]),
                        "ACCEL_TIME": jsonData["ACCEL_TIME"],
                        "ACCEL_X": float(jsonData["ACCEL_X"]),
                        "ACCEL_Y": float(jsonData["ACCEL_Y"]),
                        "ACCEL_Z": float(jsonData["ACCEL_Z"]),
                        "matrix_R": jsonData["matrix_R"],
                        "matrix_I": jsonData["matrix_I"],
                    }
            
            
            # select the collection and post the data
            collection = db[combinedCollection]
            collection.insert(record)
        
        
            return format_message("Server: GPS data stored but no WiFi access points included in post.")

        else:
            # combine each record into a list to update the db in one go
            records = []
            wifi_records = []
            for mac, strength in zip(MacAddressesJSON, signalStregthsJSON):
                records.append(
                    {
                        "UUID": jsonData["UUID"],
                        "MESSAGE": jsonData["MESSAGE"],
                        "WIFI_TIME": jsonData["WIFI_TIME"],
                        "Macs": mac,
                        "level": int(strength),
                        "GPS_TIME": jsonData["GPS_TIME"],
                        "x": float(jsonData["X"]),
                        "y": float(jsonData["Y"]),
                        "z": float(jsonData["ALTITUDE"]),
                        "acc": float(jsonData["ACC"]),
                        "MAG_TIME": jsonData["MAG_TIME"],
                        "MAG_x": float(jsonData["MAG_X"]),
                        "MAG_y": float(jsonData["MAG_Y"]),
                        "MAG_z": float(jsonData["MAG_Z"]),
                        "ACCEL_TIME": jsonData["ACCEL_TIME"],
                        "ACCEL_X": float(jsonData["ACCEL_X"]),
                        "ACCEL_Y": float(jsonData["ACCEL_Y"]),
                        "ACCEL_Z": float(jsonData["ACCEL_Z"]),
                        "matrix_R": jsonData["matrix_R"],
                        "matrix_I": jsonData["matrix_I"],
                    }
                )
                # wifi_records.append(
                    # {
                        # "UUID": jsonData["UUID"],
                        # "WIFI_TIME": jsonData["WIFI_TIME"],
                        # "Macs": mac,
                        # "level": int(strength),
                    # }
                # )

            # select the collection and post the data
            # collection = db[wifiCollection]
            # collection.insert_many(wifi_records)

            # select the collection and post the data
            collection = db[combinedCollection]
            collection.insert_many(records)
            
            return format_message("Server: Combined GPS and WiFi data stored successfully.")

    return DEFAULT_GET_RESPONSE

#-------------------------------------------------------------------------------------

@app.route("/gps/", methods=["GET", "POST"])
def gps():
    if request.method == "POST":
        # ---- Error checking

        # check we can parse the request
        try:
            jsonData = parse_request(request)
        except Exception as e:
            return print_and_jsonify(e)

        # check all the required fields are there
        for key in KEYS_REQUIRED_FOR_GPS:
            if key not in jsonData.keys():
                msg = f"Missing: {key:s}"
                return print_and_jsonify(msg)

        # check the magic number matches
        #if jsonData["MAGIC_NUM"] != magicNum:
        #    return print_and_jsonify("magic number mismatch")

        # ---- connect to the db
        client = MongoClient("mongodb://root:rootpassword@mongo", 27017)
        if "DATABASE" in jsonData.keys():
            db = client[jsonData["DATABASE"]]
        else:
            db = client[dataBase]


        # ---- post data to the GPS table
        collection = db[gpsCollection]
        collection.insert_one(
            {
                "UUID": jsonData["UUID"],
                "GPS_TIME": jsonData["GPS_TIME"],
                "x": float(jsonData["X"]),
                "y": float(jsonData["Y"]),
                "z": float(jsonData["ALTITUDE"]),
                "acc": float(jsonData["ACC"]),
                "message": jsonData["MESSAGE"],
            }
        )
        return format_message("Server: GPS data stored successfully.")

    return DEFAULT_GET_RESPONSE

#-------------------------------------------------------------------------------------

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
        #if jsonData["MAGIC_NUM"] != magicNum:
        #    return print_and_jsonify("magic number mismatch")

        # ---- connect to the db
        client = MongoClient("mongodb://root:rootpassword@mongo", 27017)
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
            return format_message("Server: No WiFi access points included in post.")

        else:
            # combine each record into a list to update the db in one go
            records = []
            for mac, strength in zip(MacAddressesJSON, signalStregthsJSON):
                records.append(
                    {
                        "UUID": jsonData["UUID"],
                        "WIFI_TIME": jsonData["WIFI_TIME"],
                        "Macs": mac,
                        "level": int(strength),
                        "message": jsonData["MESSAGE"],
                    }
                )

            # select the collection and post the data
            collection = db[wifiCollection]
            collection.insert_many(records)
            return format_message("Server: WiFi data stored successfully.")

    return DEFAULT_GET_RESPONSE

#-------------------------------------------------------------------------------------
@app.route("/ble/", methods=["GET", "POST"])
def ble():
    if request.method == "POST":
        # ---- Error checking

        # check we can parse the request
        try:
            jsonData = parse_request(request)
        except Exception as e:
            return print_and_jsonify(e)

        # check all the required fields are there
        for key in KEYS_REQUIRED_FOR_BLE:
            if key not in jsonData.keys():
                msg = f"Missing: {key:s}"
                return print_and_jsonify(msg)

        # check the magic number matches
        #if jsonData["MAGIC_NUM"] != magicNum:
        #    return print_and_jsonify("magic number mismatch")

        # ---- connect to the db
        client = MongoClient("mongodb://root:rootpassword@mongo", 27017)
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
            return format_message("Server: No BLE Beacons included in post.")

        else:
            # combine each record into a list to update the db in one go
            records = []
            for mac, strength in zip(MacAddressesJSON, signalStregthsJSON):
                records.append(
                    {
                        "UUID": jsonData["UUID"],
                        "BLE_TIME": jsonData["BLE_TIME"],
                        "Macs": mac,
                        "level": int(strength),
                        "message": jsonData["MESSAGE"],
                    }
                )

            # select the collection and post the data
            collection = db[bleCollection]
            collection.insert_many(records)
            return format_message("Server: BLE data stored successfully.")

    return DEFAULT_GET_RESPONSE

#-------------------------------------------------------------------------------------
@app.route("/mag/", methods=["GET", "POST"])
def mag():
    if request.method == "POST":
        # ---- Error checking

        # check we can parse the request
        try:
            jsonData = parse_request(request)
        except Exception as e:
            return print_and_jsonify(e)

        # check all the required fields are there
        for key in KEYS_REQUIRED_FOR_MAG:
            if key not in jsonData.keys():
                msg = f"Missing: {key:s}"
                return print_and_jsonify(msg)

        # check the magic number matches
        #if jsonData["MAGIC_NUM"] != magicNum:
        #    return print_and_jsonify("magic number mismatch")

        # ---- connect to the db
        client = MongoClient("mongodb://root:rootpassword@mongo", 27017)
        if "DATABASE" in jsonData.keys():
            db = client[jsonData["DATABASE"]]
        else:
            db = client[dataBase]

        # ---- post data to the MAG table

        # combine each record into a list to update the db in one go
        record = {
            "UUID": jsonData["UUID"],
            "MAG_TIME": jsonData["MAG_TIME"],
            "MAG_x": float(jsonData["MAG_X"]),
            "MAG_y": float(jsonData["MAG_Y"]),
            "MAG_z": float(jsonData["MAG_Z"]),
            "message": jsonData["MESSAGE"],
            }


        # select the collection and post the data if there is any
        collection = db[magneticCollection]
        
        collection.insert(record)

        return format_message("Server: Magnetic data stored successfully.")
    return DEFAULT_GET_RESPONSE
    
    
#-------------------------------------------------------------------------------------
@app.route("/accel/", methods=["GET", "POST"])
def accel():
    if request.method == "POST":
        # ---- Error checking

        # check we can parse the request
        try:
            jsonData = parse_request(request)
        except Exception as e:
            return print_and_jsonify(e)

        # check all the required fields are there
        for key in KEYS_REQUIRED_FOR_ACCEL:
            if key not in jsonData.keys():
                msg = f"Missing: {key:s}"
                return print_and_jsonify(msg)

        # check the magic number matches
        #if jsonData["MAGIC_NUM"] != magicNum:
        #    return print_and_jsonify("magic number mismatch")

        # ---- connect to the db
        client = MongoClient("mongodb://root:rootpassword@mongo", 27017)
        if "DATABASE" in jsonData.keys():
            db = client[jsonData["DATABASE"]]
        else:
            db = client[dataBase]

        # ---- post data to the MAG table

        # combine each record into a list to update the db in one go
        record = {
            "UUID": jsonData["UUID"],
            "ACCEL_TIME": jsonData["ACCEL_TIME"],
            "ACCEL_X": float(jsonData["ACCEL_X"]),
            "ACCEL_Y": float(jsonData["ACCEL_Y"]),
            "ACCEL_Z": float(jsonData["ACCEL_Z"]),
            "message": jsonData["MESSAGE"],
            }


        # select the collection and post the data if there is any
        collection = db[accelCollection]
        
        collection.insert(record)

        return format_message("Server: Accelerometer data stored successfully.")
    return DEFAULT_GET_RESPONSE

    

#-------------------------------------------------------------------------------------
######################################################################################

#if __name__ == "__main__":
    #app.run(host="0.0.0.0", debug=True)
    