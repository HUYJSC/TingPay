//
//  TingPayApp.swift
//  TingPay iOS
//
//  Created by TingPay on 2026.
//

import SwiftUI

@main
struct TingPayApp: App {
    @StateObject private var audioEngine = TingPayAudioEngine()

    var body: some Scene {
        WindowGroup {
            HomeView()
                .environmentObject(audioEngine)
                .preferredColorScheme(.light)
        }
    }
}
