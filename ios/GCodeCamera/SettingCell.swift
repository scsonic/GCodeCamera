//
//  SettingCell.swift
//  GCodeCamera
//
//  Created by 郭 又鋼 on 2015/5/22.
//  Copyright (c) 2015年 郭 又鋼. All rights reserved.
//

import Foundation
import UIKit


class SettingCell:UITableViewCell {
    
    @IBOutlet var lbSubTitle: UILabel!
    @IBOutlet var lbTitle: UILabel!
    
    var isPromptText:Bool = true ;
    var list:[String] = [] ;
    
    var itemName:String?
    var value:String = "" ;
    
    
    func setItem( name:String, value:String, list:[String], isPrompt:Bool )
    {
        self.value = value ;
        self.itemName = name ;
        self.list = list ;
        self.isPromptText = isPrompt ;
    }
    
}