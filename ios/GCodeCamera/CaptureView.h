#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>
#import <QuartzCore/CALayer.h>


@interface CaptureView : UIView {
@private
    UIImage *_imageCapture;
}

@property(nonatomic, retain) UIImage *imageCapture;

// Init
- (id)initWithView:(UIView *)view;

@end