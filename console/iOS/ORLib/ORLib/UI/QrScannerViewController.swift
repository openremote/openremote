//
//  QrScannerViewController.swift
//  ORLib
//
//  Created by Michael Rademaker on 22/11/2021.
//

import AVFoundation
import UIKit

public protocol QrScannerDelegate: AnyObject {
    func codeScanned(_ codeContents:String?)
}

public class QrScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var captureSession: AVCaptureSession!
    var previewLayer: ScannerOverlayPreviewLayer!
    public var delegate: QrScannerDelegate?

    override public func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.black
        captureSession = AVCaptureSession()

        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else { return }
        let videoInput: AVCaptureDeviceInput

        do {
            videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
        } catch {
            return
        }

        if (captureSession.canAddInput(videoInput)) {
            captureSession.addInput(videoInput)
        } else {
            failed()
            return
        }

        let metadataOutput = AVCaptureMetadataOutput()

        if (captureSession.canAddOutput(metadataOutput)) {
            captureSession.addOutput(metadataOutput)

            metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [.qr]
        } else {
            failed()
            return
        }

        previewLayer = ScannerOverlayPreviewLayer(session: captureSession)
        previewLayer.frame = view.layer.bounds
        previewLayer.videoGravity = .resizeAspectFill
        previewLayer.backgroundColor = UIColor.black.withAlphaComponent(0.7).cgColor
        previewLayer.cornerRadius = 5
        metadataOutput.rectOfInterest = previewLayer.rectOfInterest

        let subView = UIView(frame: view.frame)

        subView.layer.addSublayer(previewLayer)
        view.addSubview(subView)
        
        DispatchQueue.global(qos: .background).async {
            self.captureSession.startRunning()
        }

        let cancelButton = UIButton(type: .close)
        cancelButton.layer.cornerRadius = 25
        cancelButton.backgroundColor = .white.withAlphaComponent(0.7)
        cancelButton.setTitleColor(.black, for: [.normal, .highlighted, .selected])
        cancelButton.addTarget(self, action: #selector(cancelButtonTapped), for: .touchUpInside)
        cancelButton.frame = CGRect(x: 20, y: 20, width: 48, height: 48)
        view.addSubview(cancelButton)
    }

    @objc func cancelButtonTapped() {
        if (delegate != nil) {
            delegate?.codeScanned(nil)
        } else {
            self.dismiss(animated: true, completion: nil)
        }
    }

    func failed() {
        let ac = UIAlertController(title: "Scanning not supported", message: "Your device does not support scanning a code from an item. Please use a device with a camera.", preferredStyle: .alert)
        ac.addAction(UIAlertAction(title: "OK", style: .default))
        present(ac, animated: true)
        captureSession = nil
    }

    override public func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        if (captureSession?.isRunning == false) {
            DispatchQueue.global(qos: .background).async {
                self.captureSession.startRunning()
            }
        }
    }

    override public func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

        if (captureSession?.isRunning == true) {
            captureSession.stopRunning()
        }
    }

    public func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        captureSession.stopRunning()

        if let metadataObject = metadataObjects.first {
            guard let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject else { return }
            guard let stringValue = readableObject.stringValue else { return }
            AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
            delegate?.codeScanned(stringValue)
        }
    }

    override public var prefersStatusBarHidden: Bool {
        return true
    }

    override public var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return .portrait
    }
}
