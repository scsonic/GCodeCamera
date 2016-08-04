//
//  GCodeParser.swift
//  GCodeCamera
//
//  Created by 郭 又鋼 on 2015/6/2.
//  Copyright (c) 2015年 郭 又鋼. All rights reserved.
//

import Foundation


class GCodeParser {
    
    
    var set:Setting
    var caller:SettingController
    var url:NSURL ;
    
    var path:String ;
    
    var layerHeight:String?
    var temp:String?
    var speed:String?
    
    var running = true ;
    
    var maxExtrude:Float = 0 ;
    var feedrate:Float = 0 ; // currnet feed rate
    var zheight:Float = 0 ; // current zheight ;
    
    var fdMap:NSMutableDictionary = NSMutableDictionary() ;
    var zeMap:NSMutableDictionary = NSMutableDictionary() ;
    
    var zList:[Float] = [] ;
    var zDiffList:[Float] = [] ;
    
    var thread:NSThread?
    var fileSize:UInt64 ;
    var isJumpedToCenter:Bool = false ;
    
    
    var startParseTime:NSDate?
    var lastProecessTime:NSDate = NSDate()
    var processMaxTime = -10 ;
    var maxLine:Int = 300 ;
    var lines:Int = 0 ;
    
    
    init( view:SettingController, set:Setting, url:NSURL) {
        self.set = set ;
        self.caller = view ;
        self.url = url ;
        
        self.path = url.path! ;
        
        do {
            var attr:NSDictionary? = try NSFileManager.defaultManager().attributesOfItemAtPath(self.path)
            if let _attr = attr {
                fileSize = _attr.fileSize();
                print("file size=\(fileSize)")
            }
            else {
                fileSize = 0 ;
            }
        }
        catch _ {
            fileSize = 0 ;
        }
    }
    

    func parse()
    {
        startParseTime = NSDate() ;
        print("file path=\( self.url.path )" )
        if var br = StreamReader(path: self.path) {
            while var line = br.nextLine() {
                var line = line.stringByTrimmingCharactersInSet(NSCharacterSet.newlineCharacterSet())
                //print(line)
                
                lines = lines + 1
                
                /*
                maxLine-- ;
                if maxLine < 0
                    { break ; }
                */
                
                
                if ( self.layerHeight == nil )
                {
                    var str:String? ;
                    str = self.findStringIn(line, start: "Layer height: ", end: " ") ;
                    if ( str != nil && self.isFloat( str! ))
                    {
                        self.layerHeight = str ;
                        print("@@ Parse Height = \(self.layerHeight)") ;
                    }
                    
                    str = self.findStringAfter(line, start: "; layer_height = " ) ;
                    if ( str != nil && self.isFloat( str! ))
                    {
                        self.layerHeight = str ;
                        print("@@ Parse Height = \(self.layerHeight)") ;
                    }
                    
                    str = self.findStringAfter(line, start: "; layer_thickness_mm = " ) ;
                    if ( str != nil && self.isFloat( str! ))
                    {
                        self.layerHeight = str ;
                        print("@@ Parse Height = \(self.layerHeight)") ;
                    }
                    else if ( str != nil )
                    {
                        print( "str != nil but isFloat fail [\(str)]") ;
                    }
                }
                
                if self.temp == nil {
                    var str = self.isCmdArgStartWith(line, cmd: "M109", startWidth: "S") ;
                    if ( str != nil )
                    {
                        if let fl = self.parseArg( str! ) {
                            self.temp = "\(fl)"
                            print(" find temp = \( self.temp )") ;
                        }
                    }
                }
                
                self.parseLayerHeightAndSpeed(line) ;
                
                
                if lastProecessTime.timeIntervalSinceNow < -1  {
                    lastProecessTime = NSDate() ;
                    print(" parse lines: \(lines)")
                }
                
                if ( self.layerHeight != nil && self.temp != nil ) { //二個都有人才給跳 xd
                    if Int(startParseTime!.timeIntervalSinceNow) < ( self.processMaxTime / 5) {
                        if ( fileSize != 0  && isJumpedToCenter == false ) {
                            print("強制跳到處理中間") ;
                            br.rewindTo( (self.fileSize / 2) as UInt64) ;
                            br.nextLine() ; // 跳過一行 xd
                            isJumpedToCenter = true
                        }
                    }
                }
                if Int(startParseTime!.timeIntervalSinceNow) < self.processMaxTime {
                    print("太久了 強制停止 已parse lines:\( lines )")
                    break ;
                }
            }
            br.close()
            
            
            // debug print
            print("zList size=\(self.zList.count)") ;
            print("zeMap = \( zeMap.count )")
            print("fdMap = \( fdMap.count )")
            
            // 不能移 有計算在裡面

            

            if self.layerHeight == nil  {
                // 需要從data中拿資料
                print("avgZDiff=\( self.getAvgZDiff() ) ") ;
                self.zDiffList.sort({ $0 > $1 })
                self.layerHeight = formatFloat( self.getManyInArray( self.zDiffList ) );
            }
            
            if ( self.speed == nil ) {
                self.speed = formatFloat( getMaxFeedRate() / 60.0 ) ;
                print("max feedrate = \(  self.speed )") ;

            }
            
            print("layer height=\(self.layerHeight)") ;
            print("speed = \( self.speed) " ) ;
            print("temp = \( self.temp ) ") ;
            
        }
        else {
            fail("can't open file @@") ;
        }
        
    }
    
    func formatFloat( f:Float) -> String {
        return (NSString(format: "%.2f", f)) as String
    }
    
    func parseLayerHeightAndSpeed(line:String) {
        
        if ( line.startWith("G0") || line.startWith("G1") )
        {
            var args:[String] = line.split(" |;|,|:") ;
            if line.contains("Z") {
                for arg:String in args {
                    if ( arg.startWith("Z") && arg.lengthOfBytesUsingEncoding(NSUTF8StringEncoding) > 1 )
                    {
                        if var f = self.parseArg(arg) {
                            self.zheight = f ;
                            self.zList.append( self.zheight ) ;
                        }
                    }
                }
            }

            
            var feedStr = self.getStartWithInArray( args, startWith: "F") ;
            if ( feedStr != nil )
            {
                if var f = self.parseArg( feedStr! ) {
                    self.feedrate = f ;
                }
            }
            
            if var extrudeStr = getStartWithInArray(args, startWith: "E") {
                if var newE:Float = self.parseArg(extrudeStr) {
                    if ( newE > maxExtrude && (newE - maxExtrude < 10) ) {
                        if fdMap.objectForKey(self.feedrate) == nil {
                            fdMap[self.feedrate] = 0.0
                        }
                        var feedlength:Float = fdMap.objectForKey(self.feedrate) as! Float
                        fdMap[self.feedrate] = feedlength + (newE - maxExtrude)
                        
                        if ( zeMap.objectForKey(self.zheight) == nil )
                        {
                            zeMap[self.zheight] = 0.0 ;
                        }
                        var zelength:Float = zeMap.objectForKey(self.zheight) as! Float
                        zeMap[ self.zheight] = zelength + ( newE - maxExtrude)
                        maxExtrude = newE ;
                        
                    }
                    else if (newE > maxExtrude && (newE - maxExtrude > 10)) {
                        //回抽 @@
                        print("skip 跳過的extrude 的啦 不然計算會錯誤") ;
                        maxExtrude = newE ; // 設定新的就好 不要再計算了 xdxd
                    
                    }
                    else {
                        // 回抽 @@
                    }
                }

            }
        }
    }
    
    func parseArg(arg:String) -> Float?
    {
        
        
        let value = arg.substringWithRange( Range<String.Index>(start: arg.startIndex.advancedBy(1), end: arg.endIndex))
        if ( isFloat( value )) {
            return (value as NSString).floatValue ;
        }
        else {
            return nil ;
        }
    }
    
    func fail(msg:String) {
        print(" pares fail:\(msg)") ;
    }
    
    
    
    func findStringIn( line:String, start:String, end:String) -> String? {
        var startAt = line.rangeOfString(start) ;
        
        if ( startAt != nil )
        {
            var range = Range<String.Index>(start: startAt!.endIndex, end: line.endIndex)
            var endAt = line.rangeOfString(end, options: NSStringCompareOptions.CaseInsensitiveSearch, range: range, locale: nil)
            // var endAt = line.rangeOfString(end, options: nil, range: range, locale: nil) ;
            
            if ( endAt != nil )
            {
                return line.substringWithRange(Range<String.Index>(start: startAt!.endIndex, end: endAt!.startIndex))
            }
        }
        return nil ;
    }
    
    func getStartWithInArray( arr:[String], startWith:String) -> String? {
        for a in arr {
            if a.startWith(startWith) {
                return a ;
            }
        }
        return nil ;
    }
    
    func findStringAfter( line:String, start:String) -> String? {
        var startAt = line.rangeOfString(start) ;
        
        if ( startAt != nil )
        {
            return line.substringWithRange(Range<String.Index>(start: startAt!.endIndex, end: line.endIndex))
        }
        return nil ;
    }
    
    func isCmdArgStartWith(line:String, cmd:String, startWidth:String )-> String?
    {
        if line.startWith(cmd) {
            var arr = line.split(" |;|,|:")
            
            for arg:String in arr {
                if arg.startWith( startWidth ) {
                    return arg ;
                }
            }
        }
        return nil ;
    }
    
    func isFloat(str:String) -> Bool {
        
        let numberFormatter = NSNumberFormatter()
        let number = numberFormatter.numberFromString(str)
        if ( number != nil ) {
            return true ;
        }
        else {
            return false ;
        }
    }
    
    func getAvgZDiff() -> Float{
        if zList.count <= 1 {
            return 0 ;
        }
        var sum:Float = 0 ;
        var pre:Float = zList[0] ;
        
        for i in 1 ... zList.count - 1 {
            var diff:Float = zList[i] - zList[ i - 1 ] ;
            if ( diff < 0 ) {
                continue ;
            }
            self.zDiffList.append( diff )
            sum += diff ;
        }
        
        return sum / Float(self.zList.count - 1 )
    }
    
    func getManyInArray( arr:[Float] ) -> Float {

        
        var many:Float = -1 ;
        var count = 0 ;
        for f:Float in arr {
            if ( f > 0.4 ) {
                continue ;
            }
            
            if f == many {
                count = count + 1
            }
            else {
                count = 0 ;
                many = f
            }
        }
        
        return many ;
    }
    
    func getMaxFeedRate() -> Float {
        var max:Float = 0 ;
        var maxfd:Float = 0 ;
        for (key, value ) in self.fdMap {

            if let f:Float = value as? Float {
                print("feedrate = \(key) \(value)")
                if ( f > max )
                {
                    max = f ;
                    maxfd = key as! Float ;
                }
            }
        }
        
        return maxfd ;
    }
}

