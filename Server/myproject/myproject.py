from flask import Flask, request
import json
from pymongo import MongoClient


app = Flask(__name__)

magicNum = "aaz0p3DuHxgxqNOk40XA4csgjeEgJzC7AUEb40gTZXgtAM5TtpleDwdGkbXQICmKwCxuO2WXawQQiobWd3nggGH9plwgJHyERBF9"
dataBase = 'phoneTest_home'

#client = MongoClient('localhost',27017)
#db = client[dataBase]
#collection = db['myCollection']

wifiCollection = 'wifi'
combinedCollection = 'combiColl'
gpsCollection = 'gpsColl'


@app.route("/", methods=['GET', 'POST'])
def hello():
    if request.method == 'POST':
    
        
        # get the data out of the immutable Dict object
        fields = list(request.form.to_dict().keys())[0]

        client = MongoClient('localhost',27017)
        db = client[dataBase]
        collection = db[combinedCollection]
        
        
        
        try:
            jsonData = json.loads(fields)
        except ValueError as e:
            return
        
        #-------------------------------------------
        
        if("MAGIC_NUM" not in jsonData.keys()):
            print("no magic number")
            return
        
        if(jsonData["MAGIC_NUM"]!=magicNum):
            print("magic number mismatch")
            return
            
        if("UUID" not in jsonData.keys()):
            print("no UUID")
            #print("UUID:"+jsonData["UUID"])
            return
        
        if("TIME" not in jsonData.keys()):
            print("no TIME")
            #print("TIME:"+jsonData["TIME"])
            return
            
        if("GPSTIME" not in jsonData.keys()):
            print("no GPSTIME")
            #print("GPSTIME:"+jsonData["GPSTIME"])
            return
            
        if("X" not in jsonData.keys()):
            print("no X")
            return
        
        if("Y" not in jsonData.keys()):
            print("no Y")
            return
            
        if("ALTITUDE" not in jsonData.keys()):
            print("no ALTITUDE")
            return
        
        if("MacAddressesJson" not in jsonData.keys()):
            print("no MacAddresses")
            return
        
        #===========
        
        if("DATABASE" in jsonData.keys()):
            #print("")
            db = client[jsonData["DATABASE"]]
            collection = db[combinedCollection]
        #-------------------------------------------
        
        MacAddressesJSON = json.loads(jsonData["MacAddressesJson"])
        signalStregthsJSON = json.loads(jsonData["signalStrengthsJson"])
        
        if("MacAddressesJson" not in jsonData.keys()):
            print("error reading mac addresses")
            return
        
        #-------------------------------------------
        '''
        # loop mac addresses
        for i in range(len(MacAddressesJSON)):
        
            macAddresse = MacAddressesJSON[i]
            signalStregth = signalStregthsJSON[i]
        
            collection.update_many({"phone":macAddresse},
            { "$push": { "points": { 
                "Time":jsonData["TIME"],
                "GPSTIME" : jsonData["GPSTIME"],
                "x":float(jsonData["X"]),
                "y":float(jsonData["Y"]),
                "z":float(jsonData["ALTITUDE"]),
                "acc":float(jsonData["ACC"]),
                "level":int(signalStregth),
                "uuid":jsonData["UUID"]
                } } 
                }
            ,upsert=True
            )
        
        
        '''
        
        
        
        records = []
        
        # loop mac addresses
        for i in range(len(MacAddressesJSON)):
        
            macAddresse = MacAddressesJSON[i]
            signalStregth = signalStregthsJSON[i]
        
            records.append(
                { 
                    "UUID":jsonData["UUID"],
                    "Time" : jsonData["TIME"],
                    "Macs" : macAddresse,
                    "level":int(signalStregth),
                    
                    "GPSTIME" : jsonData["GPSTIME"],
                    "x":float(jsonData["X"]),
                    "y":float(jsonData["Y"]),
                    "z":float(jsonData["ALTITUDE"]),
                    "acc":float(jsonData["ACC"])
                    
                } 
            )
        
        
        if(len(MacAddressesJSON)>0):
            collection.insert_many(
                records
                
                )
        
        
        #-------------------------------------------
        collection = db[gpsCollection]
        
        
        collection.insert_one(
            {
                "UUID":jsonData["UUID"],
            
                "GPSTIME" : jsonData["GPSTIME"],
                "x":float(jsonData["X"]),
                "y":float(jsonData["Y"]),
                "z":float(jsonData["ALTITUDE"]),
                "acc":float(jsonData["ACC"])
            }
            )
        
        
        
        
        #-------------------------------------------
        return "Data registered!"
    return "<h1 style='color:blue'>RIBA2Reailty Server Active!</h1>"

@app.route("/wifi/", methods=['GET', 'POST'])
def wifi():
    if request.method == 'POST':
    
        
        # get the data out of the immutable Dict object
        fields = list(request.form.to_dict().keys())[0]

        client = MongoClient('localhost',27017)
        db = client[dataBase]
        collection = db[wifiCollection]
        
        
        
        try:
            jsonData = json.loads(fields)
        except ValueError as e:
            return
        
        #-------------------------------------------
        
        if("MAGIC_NUM" not in jsonData.keys()):
            print("no magic number")
            return
        
        if(jsonData["MAGIC_NUM"]!=magicNum):
            print("magic number mismatch")
            return
        
        
        if("UUID" not in jsonData.keys()):
            print("no UUID")
            #print("UUID:"+jsonData["UUID"])
            return
        
        if("TIME" not in jsonData.keys()):
            print("no TIME")
            #print("TIME:"+jsonData["TIME"])
            return
            
        
        if("MacAddressesJson" not in jsonData.keys()):
            print("no MacAddresses")
            return
        
        #===========
        
        if("DATABASE" in jsonData.keys()):
            #print("")
            db = client[jsonData["DATABASE"]]
            collection = db[wifiCollection]
        #-------------------------------------------
        
        MacAddressesJSON = json.loads(jsonData["MacAddressesJson"])
        signalStregthsJSON = json.loads(jsonData["signalStrengthsJson"])
        
        if("MacAddressesJson" not in jsonData.keys()):
            print("error reading mac addresses")
            return
        
        #-------------------------------------------
        
        
        records = []
        
        # loop mac addresses
        for i in range(len(MacAddressesJSON)):
        
            macAddresse = MacAddressesJSON[i]
            signalStregth = signalStregthsJSON[i]
        
            records.append(
                { 
                    "UUID":jsonData["UUID"],
                    "Time" : jsonData["TIME"],
                    "Macs" : macAddresse,
                    "level":int(signalStregth)
                } 
            )
        
        
        if(len(MacAddressesJSON)>0):
            collection.insert_many(
                records
                
                )
            
        
        
        
        
        #-------------------------------------------
        return "Data registered!"
    return "<h1 style='color:blue'>RIBA2Reailty Server Active!</h1>"


if __name__ == "__main__":
    app.run(host='0.0.0.0',debug=True)
