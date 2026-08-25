// =========================================================
// TingPay – Giao Diện MoMo & Bộ Tự Động Phát Giọng Tiếng Việt
// TỰ ĐỘNG NHẬN DIỆN VÀ ĐỌC TIẾNG VIỆT 100% - KHÔNG CẦN CÀI ĐẶT
// =========================================================

const state = {
    todayRevenue: 0,
    txCount: 0,
    showRevenue: true,
    activeBank: {
        code: "MoMo",
        name: "Ví MoMo",
        bin: "999888",
        accountNumber: "0988776655",
        accountName: "NGUYEN VAN A"
    },
    currentOrder: {
        orderCode: "TP8291",
        amount: 0,
        desc: ""
    },
    posInput: "",
    createPayInput: "",
    voiceType: "FEMALE_NORTH",
    speechRate: 0.95,
    audioMode: "TING_AND_AMOUNT",
    transactions: [],
    audioContext: null
};

// -------------------------------------------------------------
// Âm thanh Ting Ting đặc trưng MoMo
// -------------------------------------------------------------
function playMomoTingSound() {
    try {
        if (!state.audioContext) {
            state.audioContext = new (window.AudioContext || window.webkitAudioContext)();
        }
        const ctx = state.audioContext;
        if (ctx.state === 'suspended') ctx.resume();

        // Nốt Ting 1 (1318Hz)
        const osc1 = ctx.createOscillator();
        const gain1 = ctx.createGain();
        osc1.type = 'sine';
        osc1.frequency.setValueAtTime(1318, ctx.currentTime);
        gain1.gain.setValueAtTime(0.65, ctx.currentTime);
        gain1.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.22);
        osc1.connect(gain1);
        gain1.connect(ctx.destination);
        osc1.start(ctx.currentTime);
        osc1.stop(ctx.currentTime + 0.22);

        // Nốt Ting 2 ngân vang (1760Hz)
        setTimeout(() => {
            const osc2 = ctx.createOscillator();
            const gain2 = ctx.createGain();
            osc2.type = 'sine';
            osc2.frequency.setValueAtTime(1760, ctx.currentTime);
            gain2.gain.setValueAtTime(0.75, ctx.currentTime);
            gain2.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.45);
            osc2.connect(gain2);
            gain2.connect(ctx.destination);
            osc2.start(ctx.currentTime);
            osc2.stop(ctx.currentTime + 0.45);
        }, 110);
    } catch (e) {
        console.warn("Lỗi Web Audio", e);
    }
}

// -------------------------------------------------------------
// Bộ Tự Động Quét & Khởi Tạo Giọng Đọc Tiếng Việt
// -------------------------------------------------------------
let cachedViVoice = null;

function autoDetectVietnameseVoice() {
    if (!('speechSynthesis' in window)) return;
    const voices = window.speechSynthesis.getVoices();

    // Tự động tìm giọng tiếng Việt: Google Tiếng Việt, Apple Siri Tiếng Việt (iOS), Microsoft Hoài My/Nam Minh, hoặc vi-VN
    cachedViVoice = voices.find(v => v.lang === 'vi-VN' || v.lang === 'vi_VN' || v.lang.toLowerCase().startsWith('vi')) ||
                    voices.find(v => v.name.toLowerCase().includes('vietnam') || v.name.toLowerCase().includes('tiếng việt') || v.name.toLowerCase().includes('linh') || v.name.toLowerCase().includes('an'));
}

if ('speechSynthesis' in window) {
    autoDetectVietnameseVoice();
    window.speechSynthesis.onvoiceschanged = autoDetectVietnameseVoice;
}

/**
 * Tự động phát âm thanh tiếng Việt mượt mà ngay lập tức
 */
function speakText(text) {
    if (!('speechSynthesis' in window)) return;

    window.speechSynthesis.cancel();
    const utter = new SpeechSynthesisUtterance(text);
    utter.lang = 'vi-VN'; // Tự động gán tiếng Việt

    if (!cachedViVoice) autoDetectVietnameseVoice();
    if (cachedViVoice) utter.voice = cachedViVoice;

    let pitch = 1.0;
    let rate = state.speechRate;

    switch (state.voiceType) {
        case 'FEMALE_NORTH': pitch = 1.12; break;
        case 'MALE_NORTH': pitch = 0.88; break;
        case 'FEMALE_SOUTH': pitch = 1.20; break;
        case 'MALE_SOUTH': pitch = 0.82; break;
        default: pitch = 1.0; break;
    }

    utter.pitch = pitch;
    utter.rate = rate;
    utter.volume = 1.0;

    window.speechSynthesis.speak(utter);
}

function buildSpokenSentence(amount, bankName) {
    const words = numberToVietnameseWords(amount);
    switch (state.audioMode) {
        case "FULL_MOMO": return `Bạn vừa nhận được ${words}`;
        case "BANK_AND_AMOUNT":
            const bName = bankName === "MoMo" ? "Ví MoMo" : bankName;
            return `${bName}, nhận ${words}`;
        case "TING_AND_AMOUNT":
        default: return `Đã nhận ${words}`;
    }
}

function updateSpeechPreview() {
    const amount = parseInt(document.getElementById('customAmount')?.value) || 350000;
    const bank = document.getElementById('customBank')?.value || "Ví MoMo";
    const previewEl = document.getElementById('previewSpeechText');
    if (previewEl) {
        previewEl.innerText = `"${buildSpokenSentence(amount, bank)}"`;
    }
}

function notifyAudio(amount, bankName) {
    // 1. Tự động phát chuông Ting
    playMomoTingSound();

    if (state.audioMode === "TING_ONLY") return;

    // 2. Tự động đọc câu tiếng Việt hoàn chỉnh
    setTimeout(() => {
        const sentence = buildSpokenSentence(amount, bankName);
        speakText(sentence);
    }, 520);
}

// -------------------------------------------------------------
// Chuyển Đổi Số Tiền Thành Chữ Tiếng Việt Tự Nhiên
// -------------------------------------------------------------
function numberToVietnameseWords(amount) {
    if (amount === 0) return "không đồng";
    if (amount < 0) return "âm " + numberToVietnameseWords(-amount);

    const DIGITS = ["không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"];
    const UNITS = ["", "nghìn", "triệu", "tỷ"];

    let num = amount;
    let groups = [];
    while (num > 0) {
        groups.push(num % 1000);
        num = Math.floor(num / 1000);
    }

    const groupCount = groups.length;
    let words = [];

    for (let i = groupCount - 1; i >= 0; i--) {
        const val = groups[i];
        if (val === 0) continue;

        const isHighestGroup = (i === groupCount - 1);
        const readZeroHundred = !isHighestGroup;

        const hundred = Math.floor(val / 100);
        const ten = Math.floor((val % 100) / 10);
        const unit = val % 10;
        let groupText = "";

        if (hundred > 0 || readZeroHundred) groupText += DIGITS[hundred] + " trăm ";
        if (ten > 1) groupText += DIGITS[ten] + " mươi ";
        else if (ten === 1) groupText += "mười ";
        else if (ten === 0 && unit > 0 && (hundred > 0 || readZeroHundred)) groupText += "lẻ ";

        if (unit === 1 && ten >= 2) groupText += "mốt";
        else if (unit === 4 && ten >= 2) groupText += "tư";
        else if (unit === 5 && ten >= 1) groupText += "lăm";
        else if (unit > 0) groupText += DIGITS[unit];

        const unitIndex = i % 4;
        const unitName = UNITS[unitIndex];
        const billionsMultiplier = Math.floor(i / 4);
        let extraBillions = "";
        if (billionsMultiplier > 0 && unitIndex === 0) {
            extraBillions = "tỷ ".repeat(billionsMultiplier).trim();
        }

        let part = "";
        if (extraBillions) part = `${groupText.trim()} ${extraBillions}`;
        else if (unitName) part = `${groupText.trim()} ${unitName}`;
        else part = groupText.trim();

        words.push(part.trim());
    }

    return words.join(" ").trim() + " đồng";
}

// -------------------------------------------------------------
// Sinh Mã VietQR NAPAS 247 Chuẩn EMVCo
// -------------------------------------------------------------
function crc16(data) {
    let crc = 0xFFFF;
    const bytes = new TextEncoder().encode(data);
    for (let b of bytes) {
        crc ^= (b << 8);
        for (let i = 0; i < 8; i++) {
            if ((crc & 0x8000) !== 0) {
                crc = ((crc << 1) ^ 0x1021) & 0xFFFF;
            } else {
                crc = (crc << 1) & 0xFFFF;
            }
        }
    }
    return crc.toString(16).toUpperCase().padStart(4, '0');
}

function generateVietQrPayload(bin, acc, amount, message) {
    const tlv = (tag, val) => tag + String(val.length).padStart(2, '0') + val;
    let sub38_01 = tlv("00", bin) + tlv("01", acc);
    let tag38 = tlv("00", "A000000727") + tlv("01", sub38_01) + tlv("02", "QRIBFTTA");

    let payload = tlv("00", "01") +
                  tlv("01", amount > 0 ? "12" : "11") +
                  tlv("38", tag38) +
                  tlv("53", "704");

    if (amount > 0) payload += tlv("54", String(amount));
    payload += tlv("58", "VN");

    if (message) {
        let cleanMsg = message.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-zA-Z0-9 ]/g, "").trim().substring(0, 25);
        payload += tlv("62", tlv("08", cleanMsg));
    }

    payload += "6304";
    return payload + crc16(payload);
}

// -------------------------------------------------------------
// Điều Hướng & Giao Diện
// -------------------------------------------------------------
function changeVoiceType() {
    state.voiceType = document.getElementById('voiceTypeSelect').value;
    const phoneSel = document.getElementById('phoneVoiceSelect');
    if (phoneSel) phoneSel.value = state.voiceType;
    updateSpeechPreview();
}

function syncVoiceFromPhone(val) {
    state.voiceType = val;
    const sideSel = document.getElementById('voiceTypeSelect');
    if (sideSel) sideSel.value = val;
    updateSpeechPreview();
}

function changeSpeechRate() {
    state.speechRate = parseFloat(document.getElementById('speechRateSelect').value);
    const phoneSpd = document.getElementById('phoneSpeedSelect');
    if (phoneSpd) phoneSpd.value = String(state.speechRate);
}

function syncSpeedFromPhone(val) {
    state.speechRate = parseFloat(val);
    const sideSpd = document.getElementById('speechRateSelect');
    if (sideSpd) sideSpd.value = val;
}

function changeAudioMode() {
    state.audioMode = document.getElementById('webAudioMode').value;
    updateSpeechPreview();
}

function navigateTo(screenId) {
    document.querySelectorAll('.screen-view').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    const target = document.getElementById('screen' + screenId);
    if (target) target.classList.add('active');

    const navBtn = document.getElementById('nav' + screenId);
    if (navBtn) navBtn.classList.add('active');

    if (screenId === 'Cashier') renderPosQr();
}

function formatVnd(val) {
    return new Intl.NumberFormat('vi-VN').format(val) + " đ";
}

function toggleRevenueEye() {
    state.showRevenue = !state.showRevenue;
    const el = document.getElementById('homeTodayRevenue');
    el.innerText = state.showRevenue ? formatVnd(state.todayRevenue) : "••••••• đ";
}

// Giả lập nhận tiền
function simulatePush(bankCode, amount, desc, isDebit = false) {
    const banner = document.getElementById('phoneNotiBanner');
    const icon = document.getElementById('notiIcon');
    const title = document.getElementById('notiTitle');
    const text = document.getElementById('notiText');

    icon.innerText = bankCode === "MoMo" ? "MoMo" : bankCode;
    title.innerText = isDebit ? 'Thông báo trừ tiền' : 'Nhận tiền thành công';
    text.innerText = isDebit ? `TK: -${formatVnd(amount)} | ${desc}` : `Bạn vừa nhận được +${formatVnd(amount)} từ khách hàng`;

    banner.classList.add('show');
    setTimeout(() => banner.classList.remove('show'), 3800);

    const nowTime = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

    if (!isDebit) {
        state.todayRevenue += amount;
        state.txCount += 1;

        const tx = {
            id: Date.now(),
            bankCode: bankCode,
            amount: amount,
            desc: desc,
            time: nowTime,
            type: 'CREDIT'
        };
        state.transactions.unshift(tx);

        updateDashboard();
        notifyAudio(amount, bankCode);

        const qrScreenActive = document.getElementById('screenQrView').classList.contains('active');
        const cashierActive = document.getElementById('screenCashier').classList.contains('active');

        if (qrScreenActive || cashierActive) {
            showPaymentSuccess(amount, bankCode, state.currentOrder.orderCode, nowTime);
        }
    } else {
        const tx = {
            id: Date.now(),
            bankCode: bankCode,
            amount: amount,
            desc: desc,
            time: nowTime,
            type: 'DEBIT'
        };
        state.transactions.unshift(tx);
        updateDashboard();
    }
}

function simulateCustomPush() {
    const bank = document.getElementById('customBank').value;
    const amount = parseInt(document.getElementById('customAmount').value) || 50000;
    const desc = document.getElementById('customDesc').value || "Chuyen tien";
    simulatePush(bank, amount, desc);
}

function updateDashboard() {
    const revEl = document.getElementById('homeTodayRevenue');
    revEl.innerText = state.showRevenue ? formatVnd(state.todayRevenue) : "••••••• đ";
    document.getElementById('homeTxCount').innerText = `${state.txCount} đơn`;

    const recentList = document.getElementById('homeRecentList');
    if (state.transactions.length === 0) {
        recentList.innerHTML = `<div class="empty-state">Chưa có giao dịch nào hôm nay</div>`;
    } else {
        recentList.innerHTML = state.transactions.slice(0, 4).map(tx => `
            <div class="tx-row-momo">
                <div class="tx-momo-left">
                    <div class="tx-momo-badge">${tx.bankCode.slice(0, 4)}</div>
                    <div>
                        <div class="tx-momo-desc">${tx.desc}</div>
                        <div class="tx-momo-time">${tx.time}</div>
                    </div>
                </div>
                <div class="tx-momo-amount ${tx.type === 'CREDIT' ? 'amount-plus' : 'amount-minus'}">
                    ${tx.type === 'CREDIT' ? '+' : '-'}${formatVnd(tx.amount)}
                </div>
            </div>
        `).join('');
    }

    const fullList = document.getElementById('historyFullList');
    if (fullList) {
        if (state.transactions.length === 0) {
            fullList.innerHTML = `<div class="empty-state">Chưa có giao dịch nào</div>`;
        } else {
            fullList.innerHTML = state.transactions.map(tx => `
                <div class="tx-row-momo">
                    <div class="tx-momo-left">
                        <div class="tx-momo-badge">${tx.bankCode.slice(0, 4)}</div>
                        <div>
                            <div class="tx-momo-desc">${tx.desc}</div>
                            <div class="tx-momo-time">${tx.time}</div>
                        </div>
                    </div>
                    <div class="tx-momo-amount ${tx.type === 'CREDIT' ? 'amount-plus' : 'amount-minus'}">
                        ${tx.type === 'CREDIT' ? '+' : '-'}${formatVnd(tx.amount)}
                    </div>
                </div>
            `).join('');
        }
    }
}

// Bàn phím POS
function posPress(val) {
    if (state.posInput.length < 9) {
        state.posInput += val;
        document.getElementById('posDisplayAmount').innerText = formatVnd(parseInt(state.posInput) || 0);
        renderPosQr();
    }
}

function posDel() {
    state.posInput = state.posInput.slice(0, -1);
    document.getElementById('posDisplayAmount').innerText = state.posInput ? formatVnd(parseInt(state.posInput)) : "0 đ";
    renderPosQr();
}

function posClear() {
    state.posInput = "";
    document.getElementById('posDisplayAmount').innerText = "0 đ";
    renderPosQr();
}

function posSubmit() {}

function renderPosQr() {
    const amount = parseInt(state.posInput) || 0;
    const container = document.getElementById('posQrCanvas');
    container.innerHTML = "";

    const payload = generateVietQrPayload(
        state.activeBank.bin,
        state.activeBank.accountNumber,
        amount,
        state.currentOrder.orderCode
    );

    new QRCode(container, {
        text: payload,
        width: 140,
        height: 140,
        colorDark: "#000000",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.M
    });
}

function createPayPress(val) {
    if (state.createPayInput.length < 9) {
        state.createPayInput += val;
        document.getElementById('createPayDisplay').innerText = formatVnd(parseInt(state.createPayInput) || 0);
    }
}

function createPayDel() {
    state.createPayInput = state.createPayInput.slice(0, -1);
    document.getElementById('createPayDisplay').innerText = state.createPayInput ? formatVnd(parseInt(state.createPayInput)) : "0 đ";
}

function createPayClear() {
    state.createPayInput = "";
    document.getElementById('createPayDisplay').innerText = "0 đ";
}

function generateAndShowQr() {
    const amount = parseInt(state.createPayInput) || 0;
    if (amount <= 0) return alert("Vui lòng nhập số tiền!");

    state.currentOrder.amount = amount;
    state.currentOrder.desc = document.getElementById('createPayNote').value;
    state.currentOrder.orderCode = "TP" + Math.floor(1000 + Math.random() * 9000);

    document.getElementById('qrScreenAmount').innerText = formatVnd(amount);
    document.getElementById('qrScreenAcc').innerText = `${state.activeBank.name} • ${state.activeBank.accountNumber}`;
    document.getElementById('qrScreenName').innerText = state.activeBank.accountName;
    document.getElementById('qrScreenDesc').innerText = `MÃ ĐƠN: ${state.currentOrder.orderCode}`;

    const container = document.getElementById('mainQrCanvas');
    container.innerHTML = "";

    const payload = generateVietQrPayload(
        state.activeBank.bin,
        state.activeBank.accountNumber,
        amount,
        state.currentOrder.orderCode
    );

    new QRCode(container, {
        text: payload,
        width: 190,
        height: 190,
        colorDark: "#000000",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.M
    });

    navigateTo('QrView');
}

function showPaymentSuccess(amount, bank, code, timeStr) {
    document.getElementById('successAmount').innerText = formatVnd(amount);
    document.getElementById('successBank').innerText = bank === "MoMo" ? "Ví MoMo" : `Ngân hàng ${bank}`;
    document.getElementById('successCode').innerText = code || "POS";
    document.getElementById('successTime').innerText = timeStr || "Vừa xong";
    navigateTo('Success');
}

function testSpeaker() {
    const amount = parseInt(document.getElementById('customAmount')?.value) || 350000;
    const bank = document.getElementById('customBank')?.value || "Ví MoMo";
    notifyAudio(amount, bank);
}

// Khởi tạo
setInterval(() => {
    const now = new Date();
    const timeStr = now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    document.getElementById('phoneTime').innerText = timeStr;
}, 1000);

window.addEventListener('DOMContentLoaded', () => {
    autoDetectVietnameseVoice();
    updateDashboard();
    renderPosQr();
    updateSpeechPreview();
});
