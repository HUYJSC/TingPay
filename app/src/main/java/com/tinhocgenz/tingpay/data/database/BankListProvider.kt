package com.tinhocgenz.tingpay.data.database

import com.tinhocgenz.tingpay.domain.model.BankInfo

object BankListProvider {

    val supportedBanks: List<BankInfo> = listOf(
        BankInfo("MB", "MBBank", "Ngân hàng Quân Đội", "970422", "com.mbmobile"),
        BankInfo("VCB", "Vietcombank", "Ngân hàng Ngoại Thương Việt Nam", "970436", "com.VCB"),
        BankInfo("TCB", "Techcombank", "Ngân hàng Kỹ Thương Việt Nam", "970407", "vn.com.techcombank.bb.app"),
        BankInfo("ACB", "ACB", "Ngân hàng Á Châu", "970416", "mobile.acb.com.vn"),
        BankInfo("BIDV", "BIDV", "Ngân hàng Đầu tư và Phát triển VN", "970418", "com.vnpay.bidv"),
        BankInfo("CTG", "VietinBank", "Ngân hàng Công Thương Việt Nam", "970415", "com.vietinbank.ipay"),
        BankInfo("TPB", "TPBank", "Ngân hàng Tiên Phong", "970423", "com.tpb.mb.gprsandroid"),
        BankInfo("VPB", "VPBank", "Ngân hàng Việt Nam Thịnh Vượng", "970432", "com.vnpay.vpbankonline"),
        BankInfo("STB", "Sacombank", "Ngân hàng Sài Gòn Thương Tín", "970403", "src.com.sacombank"),
        BankInfo("HDB", "HDBank", "Ngân hàng Phát triển TP.HCM", "970437", "com.vnpay.hdbank"),
        BankInfo("MSB", "MSB", "Ngân hàng Hàng Hải", "970426", "com.msb.digital"),
        BankInfo("SHB", "SHB", "Ngân hàng Sài Gòn - Hà Nội", "970443", "vn.shb.mbanking"),
        BankInfo("OCB", "OCB", "Ngân hàng Phương Đông", "970448", "com.mplus.vietnam"),
        BankInfo("VIB", "VIB", "Ngân hàng Quốc tế", "970441", "com.vib.myvib2"),
        BankInfo("SEAB", "SeABank", "Ngân hàng Đông Nam Á", "970440", "vn.com.seabank.mb1"),
        BankInfo("LPB", "LPBank", "Ngân hàng Bưu Điện Liên Việt", "970449", "com.lpbank.mobile"),
        BankInfo("ABB", "ABBANK", "Ngân hàng An Bình", "970425", "com.vnpay.abbank"),
        BankInfo("NAB", "NamABank", "Ngân hàng Nam Á", "970428", "ops.namabank.com.vn"),
        BankInfo("BAB", "BacABank", "Ngân hàng Bắc Á", "970409", "com.vnpay.bacabank"),
        BankInfo("VAB", "VietABank", "Ngân hàng Việt Á", "970427", "vn.com.vietabank.ezmobile"),
        BankInfo("VCCB", "BVBank", "Ngân hàng Bản Việt", "970454", "vn.com.vietcapitalbank.digibank"),
        BankInfo("SAIGONBANK", "SaigonBank", "Ngân hàng Sài Gòn Công Thương", "970400", "com.vnpay.sgbank"),
        BankInfo("PGB", "PGBank", "Ngân hàng Thịnh vượng và Phát triển", "970430", "com.vnpay.pgbank"),
        BankInfo("KLB", "KienlongBank", "Ngân hàng Kiên Long", "970452", "com.kienlongbank.mobilebanking"),
        BankInfo("BVB", "BaoVietBank", "Ngân hàng Bảo Việt", "970438", "com.vnpay.baovietbank"),
        BankInfo("IVB", "IVB", "Ngân hàng TNHH Indovina", "970434", "com.vnpay.ivb"),
        BankInfo("VRB", "VRB", "Ngân hàng Liên doanh Việt - Nga", "970421", "com.vnpay.vrb"),
        BankInfo("WVN", "WooriBank", "Ngân hàng Woori Việt Nam", "970457", "vn.com.woori.smart"),
        BankInfo("SHBVN", "ShinhanBank", "Ngân hàng Shinhan Việt Nam", "970442", "com.shinhan.smartbanking"),
        BankInfo("CBB", "CBBank", "Ngân hàng Xây dựng", "970444", "vn.cbbank.mobile"),
        BankInfo("GPB", "GPBank", "Ngân hàng Dầu khí Toàn cầu", "970408", "com.vnpay.gpbank"),
        BankInfo("MOMO", "MoMo", "Ví Điện Tử MoMo", "999888", "com.mservice.momotransfer"),
        BankInfo("ZALOPAY", "ZaloPay", "Ví Điện Tử ZaloPay", "999889", "vn.com.vng.zalopay")
    )

    fun findByBin(bin: String): BankInfo? = supportedBanks.firstOrNull { it.bin == bin }
    fun findByCode(code: String): BankInfo? = supportedBanks.firstOrNull { it.code.equals(code, ignoreCase = true) }
    fun findByPackage(packageName: String): BankInfo? = supportedBanks.firstOrNull { it.packageName.equals(packageName, ignoreCase = true) }
}
