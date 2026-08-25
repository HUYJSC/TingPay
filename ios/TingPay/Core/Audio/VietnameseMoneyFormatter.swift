//
//  VietnameseMoneyFormatter.swift
//  TingPay iOS
//
//  Created by TingPay on 2026.
//

import Foundation

public struct VietnameseMoneyFormatter {
    private static let digits = [
        "không", "một", "hai", "ba", "bốn",
        "năm", "sáu", "bảy", "tám", "chín"
    ]

    private static let units = ["", "nghìn", "triệu", "tỷ"]

    public static func formatToWords(amount: Int64) -> String {
        if amount == 0 { return "không đồng" }
        if amount < 0 { return "âm " + formatToWords(amount: -amount) }

        var num = amount
        var groups: [Int] = []

        while num > 0 {
            groups.append(Int(num % 1000))
            num /= 1000
        }

        let groupCount = groups.count
        var words: [String] = []

        for i in stride(from: groupCount - 1, through: 0, by: -1) {
            let groupValue = groups[i]
            if groupValue == 0 { continue }

            let isHighestGroup = (i == groupCount - 1)
            let readZeroHundred = !isHighestGroup

            let groupText = readThreeDigitsGroup(number: groupValue, readZeroHundred: readZeroHundred)
            let unitIndex = i % 4
            let unit = units[unitIndex]

            let billionsMultiplier = i / 4
            let extraBillions = (billionsMultiplier > 0 && unitIndex == 0)
                ? String(repeating: "tỷ ", count: billionsMultiplier).trimmingCharacters(in: .whitespaces)
                : ""

            let part: String
            if !extraBillions.isEmpty {
                part = "\(groupText) \(extraBillions)"
            } else if !unit.isEmpty {
                part = "\(groupText) \(unit)"
            } else {
                part = groupText
            }

            words.append(part.trimmingCharacters(in: .whitespaces))
        }

        return words.joined(separator: " ").trimmingCharacters(in: .whitespaces) + " đồng"
    }

    private static func readThreeDigitsGroup(number: Int, readZeroHundred: BooleanLiteralType) -> String {
        let hundred = number / 100
        let ten = (number % 100) / 10
        let unit = number % 10

        var sb = ""

        if hundred > 0 || readZeroHundred {
            sb += "\(digits[hundred]) trăm "
        }

        if ten > 1 {
            sb += "\(digits[ten]) mươi "
        } else if ten == 1 {
            sb += "mười "
        } else if ten == 0 && unit > 0 && (hundred > 0 || readZeroHundred) {
            sb += "lẻ "
        }

        if unit == 1 && ten >= 2 {
            sb += "mốt"
        } else if unit == 4 && ten >= 2 {
            sb += "tư"
        } else if unit == 5 && ten >= 1 {
            sb += "lăm"
        } else if unit > 0 {
            sb += digits[unit]
        }

        return sb.trimmingCharacters(in: .whitespaces)
    }
}
