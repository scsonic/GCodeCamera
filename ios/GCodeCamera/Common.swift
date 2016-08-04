//
//  Common.swift
//  GCodeCamera
//
//  Created by 郭 又鋼 on 2015/5/22.
//  Copyright (c) 2015年 郭 又鋼. All rights reserved.
//

import Foundation


class Common {
    
    struct global {
        // only one global XXD
        static var setting:Setting = Setting.readFromFile() ;
    }
    
    class func getDocumentPath() ->String {
        var paths = NSSearchPathForDirectoriesInDomains(.DocumentDirectory, .UserDomainMask, true)
        return paths[0] 
    }
    
    
    class func getPrinterList()
    {
        let url: String = "http://128.199.211.104/gcodecamera/printerlist.php" // + getStr
        let nsurl = NSURL(string: url)

        do {
            
            let t2 = try NSURLSession.sharedSession().dataTaskWithURL(nsurl!) { (res, response, error) -> Void in
                if ( error == nil )
                {
                    do {
                        let arr: NSArray? = try NSJSONSerialization.JSONObjectWithData(res!, options: NSJSONReadingOptions(rawValue: 0)) as? NSArray
                        
                        Setting.List.PRINTER_LIST.removeAll(keepCapacity: true);
                        for print in arr! {
                            //print("printer name:\(print)") ;
                            Setting.List.PRINTER_LIST.append(print as! String) ;
                        }
                        
                        Setting.Keys.list.removeAtIndex(0) ;
                        Setting.Keys.list.insert(Setting.List.PRINTER_LIST, atIndex: 0) ;
                        
                        //to do: 要提早assign list to cell 產生table時才不會沒資料
                        //所以還是要作檔案備份 一開始就從檔案取得 就沒事 反正不是第一個view
                        
                        //print("list=\(Setting.List.PRINTER_LIST)")
                    }
                    catch _ {
                        print(" to nsarray fail") ;
                    }
                }
                else
                {
                    print("err in get Http Data(網路錯誤)")
                }
            }
            
            
            t2.resume() ;
        }
        catch _ {
            
        }
    }
}