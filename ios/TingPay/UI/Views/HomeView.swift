//
//  HomeView.swift
//  TingPay iOS
//
//  Created by TingPay on 2026.
//

import SwiftUI

struct HomeView: View {
    @EnvironmentObject var audioEngine: TingPayAudioEngine
    @State private var revenueToday: Int64 = 0
    @State private var txCount: Int = 0
    @State private var showRevenue: Bool = true
    @State private var showingCashier: Bool = false

    let momoPink = Color(red: 165/255, green: 0/255, blue: 100/255)
    let momoPinkLight = Color(red: 216/255, green: 45/255, blue: 139/255)

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // MoMo Header
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        Circle()
                            .fill(Color.white)
                            .frame(width: 44, height: 44)
                            .overlay(Text("TP").bold().foregroundColor(momoPink))

                        VStack(alignment: .leading, spacing: 2) {
                            Text("TINGPAY STORE").bold().foregroundColor(.white).font(.headline)
                            Text("Cửa hàng thanh toán QR").font(.caption).foregroundColor(.white.opacity(0.85))
                        }

                        Spacer()

                        HStack(spacing: 6) {
                            Circle().fill(Color.green).frame(width: 8, height: 8)
                            Text("Đang nghe").font(.caption2).bold().foregroundColor(.white)
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.white.opacity(0.2))
                        .cornerRadius(20)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 12)
                .padding(.bottom, 24)
                .background(LinearGradient(gradient: Gradient(colors: [momoPink, momoPinkLight]), startPoint: .top, endPoint: .bottom))

                // Card Doanh Thu
                VStack(spacing: 12) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text("Doanh thu hôm nay").font(.subheadline).foregroundColor(.gray)
                                Button(action: { showRevenue.toggle() }) {
                                    Image(systemName: showRevenue ? "eye" : "eye.slash").foregroundColor(.gray)
                                }
                            }
                            Text(showRevenue ? "\(formatVnd(amount: revenueToday)) đ" : "••••••• đ")
                                .font(.system(size: 26, weight: .black))
                                .foregroundColor(momoPink)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 2) {
                            Text("Đơn thành công").font(.caption).foregroundColor(.gray)
                            Text("\(txCount) đơn").font(.headline).bold().foregroundColor(.green)
                        }
                    }
                }
                .padding(18)
                .background(Color.white)
                .cornerRadius(20)
                .shadow(color: Color.black.opacity(0.08), radius: 10, x: 0, y: 5)
                .padding(.horizontal, 16)
                .offset(y: -16)

                // 4 Action Buttons
                HStack(spacing: 12) {
                    ActionButton(title: "Thu ngân POS", icon: "creditcard.fill", color: .blue) {
                        showingCashier = true
                    }
                    ActionButton(title: "Thử loa Ting", icon: "speaker.wave.3.fill", color: momoPink) {
                        audioEngine.playPaymentAlert(amount: 350000)
                        revenueToday += 350000
                        txCount += 1
                    }
                }
                .padding(.horizontal, 16)

                Spacer()
            }
            .background(Color(red: 245/255, green: 246/255, blue: 248/255).edgesIgnoringSafeArea(.all))
            .navigationBarHidden(true)
        }
    }

    private func formatVnd(amount: Int64) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = "."
        return formatter.string(from: NSNumber(value: amount)) ?? "\(amount)"
    }
}

struct ActionButton: View {
    let title: String
    let icon: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 22))
                    .foregroundColor(color)
                    .frame(width: 50, height: 50)
                    .background(color.opacity(0.12))
                    .cornerRadius(16)

                Text(title)
                    .font(.caption)
                    .bold()
                    .foregroundColor(.primary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Color.white)
            .cornerRadius(16)
            .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 3)
        }
    }
}
