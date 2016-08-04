//
//  MyExtensions.swift
//  GCodeCamera
//
//  Created by 郭 又鋼 on 2015/5/22.
//  Copyright (c) 2015年 郭 又鋼. All rights reserved.
//

import Foundation
import UIKit

extension String {
    
    // java, javascript, PHP use 'split' name, why not in Swift? :)
    func split(splitter: String) -> Array<String> {
        
        do {
            let regEx = try NSRegularExpression(pattern: splitter, options: NSRegularExpressionOptions())
            let stop = "<SomeStringThatYouDoNotExpectToOccurInSelf>"
            let modifiedString = regEx.stringByReplacingMatchesInString (self, options: NSMatchingOptions(),
                range: NSMakeRange(0, self.lengthOfBytesUsingEncoding(NSUTF8StringEncoding)),
                withTemplate:stop)
            return modifiedString.componentsSeparatedByString(stop)
        }
        catch _ {
            print( "regular error")
        }
        return Array<String>()

    }
    
    func startWith(str:String) -> Bool 
    {
        if ( self == "" )
        {
            return false ;
        }
        if ( str.lengthOfBytesUsingEncoding(NSUTF8StringEncoding)  > self.lengthOfBytesUsingEncoding(NSUTF8StringEncoding)  )
        {
            return false
        }
        if ( str == self.substringWithRange(Range<String.Index>( start: self.startIndex, end:str.endIndex)) ) {
            return true ;
        }
        return false
    }
    
    func contains(str:String) -> Bool {
        
        if ( self.rangeOfString(str) == nil ) {
            return false ;
        }
        return true ;
    }
}