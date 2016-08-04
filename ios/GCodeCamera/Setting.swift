//
//  Setting.swift
//  GCodeCamera
//
//  Created by 郭 又鋼 on 2015/5/22.
//  Copyright (c) 2015年 郭 又鋼. All rights reserved.
//

import Foundation



class Setting {
    
    var data:NSMutableDictionary = NSMutableDictionary() ;
    
    struct Keys {
        static var printerName = "printerName" ;
        static var layerHeight = "layerHeight" ;
        static var speed = "speed" ;
        static var matrial = "matrial" ;
        static var temp = "temp" ;
        
        static var textColor = "textColor" ;
        static var textSize = "textSize" ;
        static var signature = "signature" ;
        
        static var order:[String] = [ printerName, layerHeight, speed, matrial, temp,textColor,textSize, signature ] ;
        static var list:[AnyObject] = [List.PRINTER_LIST,[],[],List.MATRIAL_LIST, [], List.COLOR_LIST, [], []]
        
        static var promptText:[Bool] = [true,true,true,true,true,false,true,true,true,false,true,true] ;
    }
    
    
    // maybe change to nsdictionary @@ fuck
    var printerName:String? ;
    var layerHeight:String?  ;
    var speed:String?
    var matrial:String?
    var temp:String? ;
    
    var textColor:String?
    var textSize:String?
    //var textPosition:String?
    
    var signature:String?
    

    func toString() -> String
    {
        // 先組成要顯示的字串
        var output = ""
        
        if let str = data[Keys.printerName] as? String{
            if ( str != "" ) {
                output =  output + str + "\n" ;
            }
        }
        
        if let str = data[Keys.matrial] as? String{
            if ( str != "" ) {
                output =  output + Keys.matrial.capitalizedString + ": " + str + "\n" ;
            }
        }
        
        if let str = data[Keys.layerHeight] as? String{
            if ( str != "" ) {
                output =  output + Keys.layerHeight.capitalizedString + ": " + str + "\n" ;
            }
        }
        
        if let str = data[Keys.speed] as? String{
            if ( str != "" ) {
                output =  output + Keys.speed.capitalizedString + ": " + str + "\n" ;
            }
        }
        
        if let str = data[Keys.temp] as? String{
            if ( str != "" ) {
                output =  output + Keys.temp.capitalizedString + ": " + str + "\n" ;
            }
        }
        

        
        if let str = data[Keys.signature] as? String{
            if ( str != "" ) {
                output = output + str  ;
            }
        }
        
        return output ;
    }
    
    func setLabel( label:UILabel, scale:CGFloat = 1 ) {
        label.text = self.toString() ;
        
        if let size = (data[Keys.textSize] as? NSString) {
            if size.integerValue >= 1 {
                label.font = UIFont(name: label.font.fontName, size: CGFloat(size.integerValue) * scale)
                label.sizeToFit()
            }
        }
        if let colorName = (data[Keys.textColor] as? String) {
            label.textColor = self.getUIColor(colorName);
        }
        

        //label.textColor = UIColor.
    }
    
    /*
    func upperCase(str:String) -> String {
        
        str.capitalizedString
    }
    */
    
    class func getDefaultSetting() -> Setting {
        let set:Setting = Setting() ;
        
        set.printerName = "My3DPrinter" ;
        set.layerHeight = "0.2" ;
        set.speed = "40";
        set.matrial = "PLA" ;
        set.textColor = "Yellow" ;
        set.textSize = "25" ;
        set.temp = nil ;
        set.signature = "AppStore: GCode Camera"

        
        set.data[Keys.printerName] = "My3DPrinter" ;
        set.data[Keys.layerHeight] = "0.2" ;
        set.data[Keys.speed] = "40" ;
        set.data[Keys.matrial] = "PLA" ;
        set.data[Keys.textColor] = "Yellow" ;
        set.data[Keys.textSize] = "15" ;
        //data[Keys.temp] = "" ;
        set.data[Keys.signature] = "AppStore: GCode Camera" ;
        
        
        return set ;
    }
    
    
    class func getGlobalPath()->String {
        return Common.getDocumentPath() + "/Setting.JSON"
    }
    
    
    /**
        從檔案存讀進來
    */
    class func readFromFile() -> Setting {
        
        if let dic = NSMutableDictionary(contentsOfFile: getGlobalPath()) {
            let set:Setting = Setting() ;
            set.data = dic ;
            return set
        }
        else
        {
            return Setting.getDefaultSetting() ;
        }
    }
    
    /**
        寫入到檔案~~ xd
    */
    class func writeSettingToFile( set:Setting )
    {
        set.data.writeToFile(getGlobalPath() , atomically: true);
        
    }
    
    
    
    func getUIColor(colorName:String ) -> UIColor
    {
        if let value = Setting.List.COLOR_DIC[colorName] as? String {
            
            return self.colorWithHexString(value) ;
        }
        else {
            return UIColor.blackColor() ;
        }
    }
    
    
    func colorWithHexString (hex:String) -> UIColor {
        var cString:String = hex.stringByTrimmingCharactersInSet(NSCharacterSet.whitespaceAndNewlineCharacterSet() as NSCharacterSet).uppercaseString
        
        if (cString.hasPrefix("#")) {
            cString = cString.substringFromIndex(cString.startIndex.advancedBy(1))
        }
        
        if (cString.lengthOfBytesUsingEncoding(NSUTF8StringEncoding) != 6) {
            return UIColor.grayColor()
        }
        
        var rgbValue:UInt32 = 0
        NSScanner(string: cString).scanHexInt(&rgbValue)
        
        return UIColor(
            red: CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0,
            green: CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0,
            blue: CGFloat(rgbValue & 0x0000FF) / 255.0,
            alpha: CGFloat(1.0)
        )
    }
    
    struct List {
        
        static var COLOR_DIC:NSDictionary = [
        "Aqua":"#00FFFF",
        "Black":"#000000",
        "Blue": "#0000FF",
        "Fuchsia":"#FF00FF",
        "Gray": "#808080",
        "Green": "#008000",
        "Lime": "#00FF00",
        "Maroon":"#800000",
        "Navy": "#000080",
        "Olive": "#808000",
        "Purple": "#800080",
        "Red": "#FF0000",
        "Silver":"#C0C0C0",
        "Teal": "#008080" ,
        "White": "#FFFFFF",
        "Yellow": "#FFFF00" ]
        
        static var COLOR_LIST:[String] = [
            "White"	,
            "Silver",
            "Gray"	,
            "Black"	,
            "Red"		,
            "Maroon"	,
            "Yellow"	,
            "Olive"	,
            "Lime"	,
            "Green"	,
            "Aqua"	,
            "Teal"	,
            "Blue"	,
            "Navy"	,
            "Fuchsia"	, 
            "Purple"
        ]

        static var SIZE_LIST:[String] = [
        "10"	,
        "20"	,
        "30"	,
        "40"	,
        "50"	,
        "60"	,
        "70"	,
        "80"
        ];
        
        static var MATRIAL_LIST:[String] = ["PLA", "ABS", "Wood", "PVA", "Nylon" ] ;
        
        static var PRINTER_LIST:[String] = []
    }
}