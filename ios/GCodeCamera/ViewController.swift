//
//  ViewController.swift
//  camera
//
//  Created by Natalia Terlecka on 10/10/14.
//  Copyright (c) 2014 imaginaryCloud. All rights reserved.
//

import UIKit
import Foundation
import AVFoundation
import MobileCoreServices
import AudioToolbox


class ViewController: UIViewController, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    
    let cameraManager = CameraManager.sharedInstance
    
    @IBOutlet weak var cameraView: UIView!
    
    @IBOutlet weak var cameraButton: UIButton!
    @IBOutlet weak var flashModeButton: UIButton!
    
    @IBOutlet weak var askForPermissionsButton: UIButton!
    @IBOutlet weak var askForPermissionsLabel: UILabel!
    
    @IBOutlet var lbPreviewText: UILabel!
    @IBOutlet var ivImportImage: UIButton!
    
    @IBOutlet var ivPreview: UIImageView!

    
    var skipFirstVolumeOberser:Bool = false ;
    
    var pick: UIImagePickerController?
    
    
    @IBAction func btnImportPress(sender: AnyObject) {
        self.pick = UIImagePickerController()
        
        if let theController = self.pick {
            theController.sourceType = UIImagePickerControllerSourceType.PhotoLibrary
            
            theController.mediaTypes = [kUTTypeImage as! String]
            theController.allowsEditing = true
            theController.delegate = self
            
            presentViewController(theController, animated: true, completion: nil)
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.cameraManager.showAccessPermissionPopupAutomatically = false
        
        self.askForPermissionsButton.hidden = true
        self.askForPermissionsLabel.hidden = true
        
        let currentCameraState = self.cameraManager.currentCameraStatus()
        
        if currentCameraState == .NotDetermined {
            self.askForPermissionsButton.hidden = false
            self.askForPermissionsLabel.hidden = false
        } else if (currentCameraState == .Ready) {
            self.addCameraToView()
        }
        if !self.cameraManager.hasFlash {
            self.flashModeButton.enabled = false
            self.flashModeButton.setTitle("No flash", forState: UIControlState.Normal)
        }
        
        

    }
    
    
    let audioSession = AVAudioSession.sharedInstance()
    func setVolumeKey()
    {
        var error: NSError?
        do {
         try audioSession.setActive(true)
        }
        catch _ {
            print( "audioSession active error")
        }

    }
    
    private struct Observation {
        static let VolumeKey = "outputVolume"
        static let Context = UnsafeMutablePointer<Void>()
    }
    
    func startObservingVolumeChanges() {
        
        //audioSession.addObserver(self, forKeyPath: Observation.VolumeKey, options: .Initial | .New, context: Observation.Context)
        audioSession.addObserver(self, forKeyPath: Observation.VolumeKey, options: NSKeyValueObservingOptions.Initial, context: Observation.Context)
    }
    
    
    override func observeValueForKeyPath(keyPath: String?, ofObject object: AnyObject?, change: [String : AnyObject]?, context: UnsafeMutablePointer<Void>) {
        if context == Observation.Context {
            if keyPath == Observation.VolumeKey, let volume = (change![NSKeyValueChangeNewKey] as? NSNumber)?.floatValue {
                
                if ( skipFirstVolumeOberser ) {
                    skipFirstVolumeOberser = false
                }
                else {
                    pressVolumeUp() ;
                }
                
                // `volume` contains the new system output volume...
            }
        } else {
            super.observeValueForKeyPath(keyPath, ofObject: object, change: change, context: context)
        }
    }
    
    /*
    override func observeValueForKeyPath(keyPath: String, ofObject object: AnyObject, change: [NSObject : AnyObject], context: UnsafeMutablePointer<Void>) {
        if context == Observation.Context {
            if keyPath == Observation.VolumeKey, let volume = (change[NSKeyValueChangeNewKey] as? NSNumber)?.floatValue {
                
                if ( skipFirstVolumeOberser ) {
                    skipFirstVolumeOberser = false
                }
                else {
                    pressVolumeUp() ;
                }

                // `volume` contains the new system output volume...
            }
        } else {
            super.observeValueForKeyPath(keyPath, ofObject: object, change: change, context: context)
        }
    }
*/
    func pressVolumeUp()
    {
        print("press volumeUp !!") ;
        self.recordButtonTapped( self.cameraButton ) ;
    }
    
    override func viewWillAppear(animated: Bool)
    {
        super.viewWillAppear(animated)
        self.cameraManager.cameraOutputMode = CameraOutputMode.StillImage
        
        self.navigationController?.navigationBar.hidden = true
        self.cameraManager.resumeCaptureSession()
        
        Common.global.setting.setLabel(self.lbPreviewText) ;
        
        skipFirstVolumeOberser = true ;
        startObservingVolumeChanges();
        setVolumeKey() ;
    }
    
    
    func imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : AnyObject]) {
        print("Picker returned successfully")
        
        let mediaType:AnyObject? = info[UIImagePickerControllerMediaType]
        
        let image = info[UIImagePickerControllerOriginalImage]
            as? UIImage
        if let theImage = image {
            //print("Image Metadata = \(theMetaData)")
            print("Image = \(theImage)")
            
            var resized = self.resizeImageV3_crop23( image! )
            
            dispatch_async(dispatch_get_main_queue(), {
                
                var textImage:UIImage = self.getLabelImage(self.lbPreviewText) ;
                self.saveImage( textImage ) ;
                var okimg = self.drawImage(resized, text: textImage);
                
                self.ivPreview.image = okimg ;
                self.ivPreview.hidden = false ;
                
                self.saveImage( okimg ) ;
            })
            
        }
        
        picker.dismissViewControllerAnimated(true, completion: nil)
    }
    /*
    pick callback
    */
    /*
    func imagePickerController(picker: UIImagePickerController,
        didFinishPickingMediaWithInfo info: [NSObject : AnyObject]){
            
            print("Picker returned successfully")
            
            let mediaType:AnyObject? = info[UIImagePickerControllerMediaType]
            
            let image = info[UIImagePickerControllerOriginalImage]
                as? UIImage
            if let theImage = image {
                //print("Image Metadata = \(theMetaData)")
                print("Image = \(theImage)")
                
                var resized = self.resizeImageV3_crop23( image! )
                
                dispatch_async(dispatch_get_main_queue(), {
                    
                    var textImage:UIImage = self.getLabelImage(self.lbPreviewText) ;
                    self.saveImage( textImage ) ;
                    var okimg = self.drawImage(resized, text: textImage);

                    self.ivPreview.image = okimg ;
                    self.ivPreview.hidden = false ;
                    
                    self.saveImage( okimg ) ;
                })
                
            }

            picker.dismissViewControllerAnimated(true, completion: nil)
    }
    */

    
    func imagePickerControllerDidCancel(picker: UIImagePickerController) {
        print("Picker was cancelled")
        picker.dismissViewControllerAnimated(true, completion: nil)
    }
    
    
    func debugAddAtImage()
    {
        var cap:CaptureView = CaptureView( view: self.lbPreviewText ) ;
        
        var image = cap.imageCapture ;
        
        print("image size=\(image)")
        
        self.ivPreview.image = image ;
        self.ivPreview.hidden = false ;
        
    }
    
    func getLabelImage( lbLabel:UILabel) -> UIImage
    {
        
        var intScale:Float = 2.0;
        var scale:CGFloat = CGFloat(intScale) ;
        var rect:CGRect = CGRect(x: 0, y: 0, width: lbLabel.bounds.width * scale, height: lbLabel.bounds.height * scale)
        
        UIGraphicsBeginImageContextWithOptions(rect.size, lbLabel.opaque, CGFloat(intScale));
        //lbLabel.layer.renderInContext(UIGraphicsGetCurrentContext()) ;
        
        //[view drawViewHierarchyInRect:view.bounds afterScreenUpdates:NO];
        // 只改這行 要畫的時候拿這個來畫 xd
        lbLabel.drawViewHierarchyInRect(rect, afterScreenUpdates: false)
    
        var image:UIImage = UIGraphicsGetImageFromCurrentImageContext() ;
        
        var img2:UIImage = UIImage(CGImage: image.CGImage!, scale: 1.0, orientation: image.imageOrientation)
        UIGraphicsEndImageContext() ;
        print("從label取得圖片= \(img2)")
        ;
        
        return img2 ;
        
    }
    
    override func viewWillDisappear(animated: Bool)
    {
        super.viewWillDisappear(animated)
        audioSession.removeObserver(self, forKeyPath: Observation.VolumeKey, context: Observation.Context)
        self.cameraManager.stopCaptureSession()
    }
    
    
    // MARK: - ViewController
    
    private func addCameraToView()
    {
        self.cameraManager.addPreviewLayerToView(self.cameraView, newCameraOutputMode: CameraOutputMode.StillImage)
        
        
        CameraManager.sharedInstance.showErrorBlock = { (erTitle: String, erMessage: String) -> Void in
            UIAlertView(title: erTitle, message: erMessage, delegate: nil, cancelButtonTitle: "OK").show()
        }
    }
    
    // MARK: - @IBActions
    
    @IBAction func changeFlashMode(sender: UIButton)
    {
        switch (self.cameraManager.changeFlashMode()) {
        case .Off:
            sender.setTitle("Off", forState: UIControlState.Normal)
            sender.setImage(UIImage(named: "ic_flash_off_36pt"), forState: UIControlState.Normal)
        case .On:
            sender.setTitle("On", forState: UIControlState.Normal)
            sender.setImage(UIImage(named: "ic_flash_on_36pt"), forState: UIControlState.Normal)
        case .Auto:
            sender.setTitle("Auto", forState: UIControlState.Normal)
            sender.setImage(UIImage(named: "ic_flash_auto_36pt"), forState: UIControlState.Normal)
        }
    }
    
    @IBAction func recordButtonTapped(sender: UIButton)
    {
        self.cameraManager.cameraOutputMode = CameraOutputMode.StillImage
        self.cameraManager.capturePictureWithCompletition({ (image, error) -> Void in
            
            print("取得相機圖片 \(image?.size)") ;
            if ( image != nil )
            {
                var resized = self.resizeImageV3_crop23( image! )
                
                //這個強制變大無效 要用另一個label 幹
                
                //self.lbHiddenText.hidden = false ;
                var textImage:UIImage = self.getLabelImage(self.lbPreviewText) ;
                //self.lbHiddenText.hidden = true ;
                //self.saveImage( textImage ) ;
    
                
                var okimg = self.drawImage(resized, text: textImage);

                self.displayImageResult( okimg ) ;
                let thread = NSThread(target: self, selector: "saveImage:", object: okimg) ;
                thread.start() ;
                // self.saveImage( okimg ) ;
            }
            else
            {
                print("image is nil @@") ;
            }
        })
    }
    
    func displayImageResult( image:UIImage) {
        self.ivPreview.image = image ;
        self.ivPreview.hidden = false ;
        
        NSTimer.scheduledTimerWithTimeInterval(0.5, target: self, selector: "hideImageResult", userInfo: nil, repeats: false)
    }
    
    func hideImageResult()
    {
        fadeOutImageView( self.ivPreview ) ;
    }
    
    func saveImage( image:UIImage )
    {
        
        if let validLibrary = self.cameraManager.library {
            let data = UIImageJPEGRepresentation(image, CGFloat(90))
            validLibrary.writeImageDataToSavedPhotosAlbum(data, metadata:nil, completionBlock: {
                (picUrl, error) -> Void in
                print("storaged image=\(image)") ;
                
                if (error != nil) {
                    
                    print("storage error") ;
                }
            })
        }
        else
        {
            print("no lib (in emulator?)") ;
        }
    }
    
    func resizeImage( image:UIImage ) -> UIImage?
    {
        
        var smallEdge:CGFloat ;
        if ( image.size.width > image.size.height )
        {
            smallEdge = image.size.height ;
        }
        else
        {
            smallEdge = image.size.width ;
        }
        var scale:CGFloat = smallEdge / 1200 ; //約就好
        
        print(" scale = \(scale)") ;
        var cropRect = CGRectMake(0,0, image.size.width / scale , image.size.height / scale) ;
        
        print("resize image rect=\(cropRect)") ;
        var imageRef = CGImageCreateWithImageInRect(image.CGImage, cropRect ) ;
        var cropped = UIImage(CGImage: imageRef!, scale: 1, orientation: image.imageOrientation)
        
        
        print("resize final image size =\(cropped.size)") ;
        return cropped
    }
    
    func resizeImageV2( image:UIImage) -> UIImage {
        
        
        var smallEdge:CGFloat ;
        if ( image.size.width > image.size.height )
        {
            smallEdge = image.size.height ;
        }
        else
        {
            smallEdge = image.size.width ;
        }
        var scale:CGFloat = smallEdge / 1200 ; //約就好
        
        print(" scale = \(scale)") ;
        var newSize:CGSize = CGSize(width: image.size.width / scale, height: image.size.height / scale)
        let rect = CGRectMake(0,0, newSize.width, newSize.height)
        
        UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
        
        // image is a variable of type UIImage
        image.drawInRect(rect)
        
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        print("縮放後大小 = \(newImage)") ;
        return newImage ;
        // resized image is stored in constant newImage
    }

    func resizeImageV3_crop23( image:UIImage) -> UIImage {
        
        
        var smallEdge:CGFloat ;
        var longEdge:CGFloat ;
        var newSize:CGSize? ;
        var rect:CGRect? ;
        if ( image.size.width > image.size.height )
        {
            smallEdge = image.size.height ;
            longEdge = image.size.width ;
            newSize = CGSize(width: 1800, height: 1200)
            rect = CGRectMake( 0,-75 , 1800, 1350)
        }
        else
        {
            smallEdge = image.size.width ;
            longEdge = image.size.height
            newSize = CGSize(width: 1200, height: 1800)
            rect = CGRectMake( -75,0 , 1350, 1800)
        }
        var scale:CGFloat = longEdge / 1800 ; //約就好
        
        // scale input to 1800 1200
        // 1800x1350 左右各減150px 即可
        
        print(" scale = \(scale)") ;
        UIGraphicsBeginImageContextWithOptions(newSize!, false, 1.0)
        
        // image is a variable of type UIImage
        image.drawInRect(rect!)
        
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        print("縮放後大小 = \(newImage)") ;
        return newImage ;
        // resized image is stored in constant newImage
    }


    
    func drawImage( image:UIImage, text:UIImage) -> UIImage
    {
        let bounds = CGRect(origin: CGPoint.zero, size: CGSize(width: image.size.width, height: image.size.height))
        let scale: CGFloat = 1
        
        UIGraphicsBeginImageContextWithOptions(image.size, false, scale)
        let context = UIGraphicsGetCurrentContext()
        
        image.drawInRect(bounds) ;
        
        
        var screenHeight = self.getScreenHeight() ;
        //var textHeight = (self.lbPreviewText.bounds.size.height * image.size.width ) / ( screenHeight ) ;
        
        
        print("screen/text = \( UIScreen.mainScreen().bounds.size ) , \(self.lbPreviewText.bounds.size ) ")
        print("image/text = \( image.size ) , \( text.size ) ")

        
        var textScale:CGFloat =  image.size.width / UIScreen.mainScreen().bounds.size.width
        
        print("screen/text height scale=\(textScale)")
        
        let toDraw = CGRect(x: 0, y: bounds.size.height - ( self.lbPreviewText.bounds.size.height
            * textScale ), width: ( self.lbPreviewText.bounds.size.width * textScale ), height: textScale * self.lbPreviewText.bounds.size.height ) ;

        print("text draw in \(toDraw)")
        text.drawInRect(toDraw) ;
        

        /*
        
        // Setup complete, do drawing here
        CGContextSetStrokeColorWithColor(context, UIColor.redColor().CGColor)
        CGContextSetLineWidth(context, 2.0)
        
        CGContextStrokeRect(context, bounds)
        
        CGContextBeginPath(context)
        CGContextMoveToPoint(context, CGRectGetMinX(bounds), CGRectGetMinY(bounds))
        CGContextAddLineToPoint(context, CGRectGetMaxX(bounds), CGRectGetMaxY(bounds))
        CGContextMoveToPoint(context, CGRectGetMaxX(bounds), CGRectGetMinY(bounds))
        CGContextAddLineToPoint(context, CGRectGetMinX(bounds), CGRectGetMaxY(bounds))
        CGContextStrokePath(context)
        
        CGContextSetStrokeColorWithColor(context, UIColor.blueColor().CGColor)
        CGContextStrokeRect(context, toDraw)
        
        CGContextBeginPath(context)
        CGContextMoveToPoint(context, CGRectGetMinX(toDraw), CGRectGetMinY(toDraw))
        CGContextAddLineToPoint(context, CGRectGetMaxX(toDraw), CGRectGetMaxY(toDraw))
        CGContextMoveToPoint(context, CGRectGetMaxX(toDraw), CGRectGetMinY(toDraw))
        CGContextAddLineToPoint(context, CGRectGetMinX(toDraw), CGRectGetMaxY(toDraw))
        CGContextStrokePath(context)
        */
        
        //CGContextDrawImage(context, toDraw, text.CGImage) ;
        
        // Drawing complete, retrieve the finished image and cleanup
        var output = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return output
    }
    
    
    func getScreenHeight() -> CGFloat
    {
        print("screen size=\(UIScreen.mainScreen().bounds) scale=\(UIScreen.mainScreen().scale)") ;
        return UIScreen.mainScreen().bounds.height ;
    }
    
    func fadeOutImageView( ivImage:UIImageView )
    {
        ivImage.hidden = false ;
        ivImage.alpha = 1.0 ;
        UIView.animateWithDuration(1.0, delay: 0.5, options: UIViewAnimationOptions.CurveEaseOut, animations: {
            ivImage.alpha = 0.0
            }, completion: {
                (finished: Bool) -> Void in
                
                ivImage.hidden = true ;
                print("completion anime") ;
        })

    }
    
    override func prefersStatusBarHidden() -> Bool {
        return true
    }
    
    @IBAction func askForCameraPermissions(sender: UIButton)
    {
        self.cameraManager.askUserForCameraPermissions({ permissionGranted in
            self.askForPermissionsButton.hidden = true
            self.askForPermissionsLabel.hidden = true
            self.askForPermissionsButton.alpha = 0
            self.askForPermissionsLabel.alpha = 0
            if permissionGranted {
                self.addCameraToView()
            }
        })
    }
    
}


