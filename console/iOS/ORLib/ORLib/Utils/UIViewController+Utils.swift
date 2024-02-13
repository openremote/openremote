import UIKit

extension UIViewController {
    
    func showToast(message : String, font: UIFont = UIFont.systemFont(ofSize: UIFont.labelFontSize)) {
        
        let toastLabel = UILabel(frame: CGRect(x: 20, y: self.view.frame.size.height - 100, width: self.view.frame.size.width - 40, height: 35))
        toastLabel.backgroundColor = UIColor.lightGray.withAlphaComponent(0.6)
        toastLabel.textColor = UIColor.darkText
        toastLabel.font = font
        toastLabel.textAlignment = .center
        toastLabel.text = message
        toastLabel.alpha = 1.0
        toastLabel.layer.cornerRadius = 10
        toastLabel.clipsToBounds  =  true
        toastLabel.sizeToFit()
        toastLabel.numberOfLines = 0
        self.view.addSubview(toastLabel)
        UIView.animate(withDuration: 4.0, delay: 0.1, options: .curveEaseOut, animations: {
            toastLabel.alpha = 0.0
        }, completion: {(isCompleted) in
            toastLabel.removeFromSuperview()
        })
    }
}
