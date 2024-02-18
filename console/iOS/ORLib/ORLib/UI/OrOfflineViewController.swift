//
//  OfflineViewController.swift
//  ORLib
//
//  Created by Michael Rademaker on 12/02/2024.
//

import UIKit

class OrOfflineViewController: UIViewController {
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let imageView = UIImageView(image: UIImage(systemName: "wifi.exclamationmark")!.withRenderingMode(.alwaysOriginal)
            .withTintColor(.black))
        
        self.view.addSubview(imageView)
        self.view.backgroundColor = .white
        
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.centerXAnchor.constraint(equalTo: self.view.centerXAnchor).isActive = true
        imageView.centerYAnchor.constraint(equalTo: self.view.centerYAnchor).isActive = true
        imageView.heightAnchor.constraint(equalToConstant: 200).isActive = true
        imageView.widthAnchor.constraint(equalToConstant: 250).isActive = true
    }
}
