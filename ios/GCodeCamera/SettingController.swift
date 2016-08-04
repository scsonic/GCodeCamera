//
//  SettingController.swift
//  GCodeCamera
//
//  Created by 郭 又鋼 on 2015/5/22.
//  Copyright (c) 2015年 郭 又鋼. All rights reserved.
//

import Foundation
import UIKit
import MobileCoreServices


class SettingController: UIViewController, UITableViewDelegate, UITableViewDataSource, UIActionSheetDelegate, UIDocumentPickerDelegate {

    var set:Setting = Common.global.setting ;
    
    var hideText = "" ;
    var otherText = "...Other..."
    var parser:GCodeParser?

    
    var dataIsNew:NSMutableDictionary = NSMutableDictionary() ;
    
    @IBOutlet var aiParseing: UIActivityIndicatorView!
    @IBOutlet var tableView: UITableView!
    
    @IBAction func importGcodePress(sender: AnyObject) {
        pickFromIOS()
    }

    @IBAction func cancelPress(sender: AnyObject) {
        Setting.writeSettingToFile(Common.global.setting) ;
        
        // back
        self.navigationController?.popToRootViewControllerAnimated(true)
    }
    

    
    override func viewDidLoad() {
        
        tableView.delegate = self
        tableView.dataSource = self
        
    }
    
    override func viewWillAppear(animated: Bool) {
        // do load table data here
        self.tableView.reloadData() ;
        self.navigationController?.navigationBar.hidden = false
        
        Common.getPrinterList() ;
        dataIsNew = NSMutableDictionary() ;
        aiParseing.hidden = true ;
    }


    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        
        return Setting.Keys.order.count ;
    }
    
    func tableView(tableView: UITableView, accessoryButtonTappedForRowWithIndexPath indexPath: NSIndexPath) {
        print("press accessory index=\( indexPath.item )")
    }
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        
        
        var cell:SettingCell = tableView.cellForRowAtIndexPath(indexPath) as! SettingCell;
        print("顯示list Menu")
        var myActionSheet = UIActionSheet()
        myActionSheet.tag = indexPath.item ;
        myActionSheet.delegate = self
    
        if ( cell.list.count > 0 ) {
            for str in cell.list {
                myActionSheet.addButtonWithTitle(str) ;
            }
            
            if cell.isPromptText == true {
                myActionSheet.addButtonWithTitle(otherText) ;
            }
                
            var cancelIndex = myActionSheet.addButtonWithTitle("取消")
            myActionSheet.cancelButtonIndex = cancelIndex
            myActionSheet.showInView(self.view)
        }
        else {
            if var oldText = cell.lbSubTitle.text {
                showPrompt( cell.lbTitle.text!, tag: indexPath.item, oldText: oldText ) ;
            }
            else {
                showPrompt( cell.lbTitle.text!, tag: indexPath.item ) ;
            }
        }
        
    }
    
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell:SettingCell = tableView.dequeueReusableCellWithIdentifier("SettingCell") as! SettingCell

        var keyname = Setting.Keys.order[indexPath.item] ;
        var value:String? = self.set.data[keyname] as? String
        var promptText:Bool = Setting.Keys.promptText[indexPath.item] ;
        
        cell.isPromptText = promptText ;
        cell.list = Setting.Keys.list[ indexPath.item ] as! [String] ;
        
        cell.lbTitle.text = keyname
        
        if ( value != nil )
        {
            cell.lbSubTitle.text = value!
        }
        else
        {
            cell.lbSubTitle.text = ""
        }
        
        if ( self.dataIsNew.objectForKey(keyname) != nil )
        {
            // show different color 
            cell.backgroundColor = UIColor.yellowColor() ;
        }
        
        cell.accessoryType = UITableViewCellAccessoryType.DisclosureIndicator
        
        return cell
    }
    
    
    func actionSheet(actionSheet: UIActionSheet, clickedButtonAtIndex buttonIndex: Int) {
        
        // 改變value with titile @@
        var title = actionSheet.buttonTitleAtIndex(buttonIndex);
        
        if ( title == otherText && title != nil ) {
            showPrompt( title!, tag: actionSheet.tag )
        }
        else {
            var keyname = Setting.Keys.order[ actionSheet.tag ] ;
            
            self.set.data[keyname] = title ;
            self.tableView.reloadData() ;
        }
    }
    
    
    var lastAlertController:TagAlertController?
    
    func showPrompt(title:String, tag:Int , oldText:String = "")
    {
        func wordEntered(alert: UIAlertAction!){
            
            if let textField:UITextField = lastAlertController?.textFields?.first {
                print("inputed=\(textField.text )") ;
                
                var keyname = Setting.Keys.order[ self.lastAlertController!.tag ] ;
                
                self.set.data[keyname] = textField.text ;
                self.tableView.reloadData() ;
            }
            
            print( "alert=\(alert)")
        }
        func addTextField(textField: UITextField!){
            textField.text = oldText
            print("entered: \( textField ) ")
        }
        
        
        // display an alert
        let newWordPrompt = TagAlertController(title: title, message: "", preferredStyle: UIAlertControllerStyle.Alert)
        lastAlertController = newWordPrompt ;
        
        newWordPrompt.tag = tag
        newWordPrompt.addTextFieldWithConfigurationHandler(addTextField)
        
        newWordPrompt.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.Default, handler: nil))
        newWordPrompt.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.Default, handler: wordEntered))
        presentViewController(newWordPrompt, animated: true, completion: nil)

    }
    
    func pickFromIOS()
    {
        let dp = UIDocumentPickerViewController(documentTypes: ["\(kUTTypeContent)","\(kUTTypeData)","\(kUTTypeItem)", "public.text", "public.data", "com.ygk.gcode"], inMode: UIDocumentPickerMode.Import);

        dp.delegate = self ;
        dp.modalPresentationStyle = UIModalPresentationStyle.FormSheet ;
        // dp.presentViewController(dp, animated: true, completion: nil) ;
        //[[UIApplication sharedApplication].keyWindow.rootViewController
        UIApplication.sharedApplication().keyWindow?.rootViewController?.presentViewController(dp, animated: true, completion: nil)
    }
    
    
    func documentPicker(controller: UIDocumentPickerViewController, didPickDocumentAtURL url: NSURL) {
        print("pickered @@ url=\(url)") ;
        
        self.aiParseing.hidden = false ;
        self.aiParseing.startAnimating() ;
        
        // read file here, no need dropbox anymore ^^ 
        
        
        if (controller.documentPickerMode == UIDocumentPickerMode.Import) {
            
            self.parser = GCodeParser(view: self, set: self.set, url: url) ;
            let thread = NSThread(target:self, selector:"doParseThread", object:nil)
            thread.start() ;
            
            /*
            NSURLRequest* request = [NSURLRequest requestWithURL:url cachePolicy:NSURLRequestReloadIgnoringLocalAndRemoteCacheData timeoutInterval:30];
            [FilePreview loadRequest:request];
            [FilePreview reload];
            */
        }
    }
    
    
    func doParseThread()
    {
        parser?.parse() ;
        dispatch_async(dispatch_get_main_queue(), {
            print("Parse OK") ;
            
            if self.parser != nil {
                if let lh = self.parser!.layerHeight {
                    Common.global.setting.data[Setting.Keys.layerHeight] = lh
                    self.dataIsNew[Setting.Keys.layerHeight] = true ;
                }
                if let te = self.parser!.temp {
                    Common.global.setting.data[Setting.Keys.temp] = te ;
                    self.dataIsNew[Setting.Keys.temp] = true ;
                }
                if let sp = self.parser!.speed {
                    Common.global.setting.data[Setting.Keys.speed] = sp ;
                    self.dataIsNew[Setting.Keys.speed] = true ;
                }
                
                self.tableView.reloadData() ;
                self.aiParseing.hidden = true ;
                self.aiParseing.stopAnimating() ;
            }
            
        })
    }
    
    func documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        print("cancel document picker XD") ;
    }
    
    // 斜斜的功能: 未來看清況使用 只有拍照是斜的
    override func shouldAutorotate() -> Bool {
        return false
    }
    override func supportedInterfaceOrientations() -> UIInterfaceOrientationMask {
//        return UIInterfaceOrientation.LandscapeLeft
        return UIInterfaceOrientationMask.LandscapeLeft
    }
}