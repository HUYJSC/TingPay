//
//  TingPayAudioEngine.swift
//  TingPay iOS
//
//  Created by TingPay on 2026.
//

import AVFoundation
import Combine

public class TingPayAudioEngine: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    private let speechSynthesizer = AVSpeechSynthesizer()
    private var audioPlayer: AVAudioPlayer?

    @Published public var isSpeaking: Bool = false
    @Published public var speechRate: Float = 0.52 // Chuẩn tự nhiên trên iOS AVSpeechSynthesizer

    override public init() {
        super.init()
        speechSynthesizer.delegate = self
        configureAudioSession()
    }

    private func configureAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(
                .playback,
                mode: .voicePrompt,
                options: [.duckOthers, .mixWithOthers]
            )
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Lỗi cấu hình AVAudioSession: \(error)")
        }
    }

    public func playPaymentAlert(amount: Int64, bankName: String = "MoMo") {
        // 1. Phát âm thanh Ting trước
        playTingSound()

        // 2. Chuyển đổi số tiền và phát giọng nói tiếng Việt sau 450ms
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) { [weak self] in
            guard let self = self else { return }
            let amountWords = VietnameseMoneyFormatter.formatToWords(amount: amount)
            let sentence = "Đã nhận \(amountWords)"
            self.speak(text: sentence)
        }
    }

    private func playTingSound() {
        AudioServicesPlaySystemSound(1057) // Nốt âm báo hệ thống iOS trong trẻo
    }

    public func speak(text: String) {
        if speechSynthesizer.isSpeaking {
            speechSynthesizer.stopSpeaking(at: .immediate)
        }

        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "vi-VN") // BẮT BUỘC vi-VN trên iOS
        utterance.rate = speechRate
        utterance.pitchMultiplier = 1.0
        utterance.volume = 1.0

        isSpeaking = true
        speechSynthesizer.speak(utterance)
    }

    public func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
    }
}
