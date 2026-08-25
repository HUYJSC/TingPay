// =========================================================
// TingPay – Bộ Xử Lý & Giả Lập Trải Nghiệm Trực Tuyến
// =========================================================

// Quản lý trạng thái ứng dụng
const state = {
    todayRevenue: 0,
    txCount: 0,
    activeBank: {
        code: "MB",
        name: "MBBank",
        bin: "970422",
        accountNumber: "0123456789",
        accountName: "NGUYEN VAN A"
    },
    currentOrder: {
        orderCode: "TP8291",
        amount: 0,
        desc: ""
    },
    posInput: "",
    createPayInput: "",
    audioMode: "TING_AND_AMOUNT",
    transactions: [],
    audioContext: null
};

// -------------------------------------------------------------
// Bộ Quản Lý Giọng Đọc Tiếng Việt Chuẩn Xác (TTS Engine)
// -------------------------------------------------------------
let cachedVietnameseVoice = null;

function initVietnameseVoices() {
    if (!('speechSynthesis' in window)) return;
    const voices = window.speechSynthesis.getVoices();
    
    cachedVietnameseVoice = voices.find(v => v.lang === 'vi-VN' || v.lang === 'vi_VN') ||
                            voices.find(v => v.lang.toLowerCase().startsWith('vi')) ||
                            voices.find(v => v.name.toLowerCase().includes('vietnam') || v.name.toLowerCase().includes('tiếng việt'));
}

if ('speechSynthesis' in window) {
    initVietnameseVoices();
    window.speechSynthesis.onvoiceschanged = initVietnameseVoices;
}

// Phát tiếng chuông "Ting" thanh thoát (Web Audio API)
function playTingSound() {
    try {
        if (!state.audioContext) {
            state.audioContext = new (window.AudioContext || window.webkitAudioContext)();
        }
        const ctx = state.audioContext;
        if (ctx.state === 'suspended') ctx.resume();

        const osc1 = ctx.createOscillator();
        const osc2 = ctx.createOscillator();
        const gain = ctx.createGain();

        osc1.type = 'sine';
        osc1.frequency.setValueAtTime(1760, ctx.currentTime);
        osc1.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.6);

        osc2.type = 'triangle';
        osc2.frequency.setValueAtTime(2637, ctx.currentTime);
        osc2.frequency.exponentialRampToValueAtTime(1318, ctx.currentTime + 0.6);

        gain.gain.setValueAtTime(0.75, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.6);

        osc1.connect(gain);
        osc2.connect(gain);
        gain.connect(ctx.destination);

        osc1.start();
        osc2.start();
        osc1.stop(ctx.currentTime + 0.6);
        osc2.stop(ctx.currentTime + 0.6);
    } catch (e) {
        console.warn("Lỗi Web Audio", e);
    }
}

// Đọc to văn bản bằng giọng tiếng Việt tự nhiên
function speakText(text) {
    if (!('speechSynthesis' in window)) return;

    window.speechSynthesis.cancel();
    const utter = new SpeechSynthesisUtterance(text);
    utter.lang = 'vi-VN';

    if (!cachedVietnameseVoice) initVietnameseVoices();
    if (cachedVietnameseVoice) utter.voice = cachedVietnameseVoice;

    utter.rate = 0.95;
    utter.pitch = 1.0;
    utter.volume = 1.0;

    window.speechSynthesis.speak(utter);
}

function notifyAudio(amount, bankName) {
    playTingSound();

    if (state.audioMode === "TING_ONLY") return;

    setTimeout(() => {
        const words = numberToVietnameseWords(amount);
        let speech = `Đã nhận ${words}`;
        if (state.audioMode === "BANK_AND_AMOUNT" && bankName) {
            speech = `${bankName}, nhận ${words}`;
        }
        speakText(speech);
    }, 450);
}

// Thuật toán tính mã kiểm tra CRC16 CCITT-FALSE cho VietQR NAPAS
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

// Xây dựng chuỗi VietQR chuẩn EMVCo Tag-Length-Value
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

// Chuyển đổi số tiền thành chữ tiếng Việt chuẩn xác (Xử lý đầy đủ: mốt, tư, lăm, lẻ, không trăm)
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

        // Hàng trăm
        if (hundred > 0 || readZeroHundred) {
            groupText += DIGITS[hundred] + " trăm ";
        }

        // Hàng chục
        if (ten > 1) {
            groupText += DIGITS[ten] + " mươi ";
        } else if (ten === 1) {
            groupText += "mười ";
        } else if (ten === 0 && unit > 0 && (hundred > 0 || readZeroHundred)) {
            groupText += "lẻ ";
        }

        // Hàng đơn vị
        if (unit === 1 && ten >= 2) {
            groupText += "mốt";
        } else if (unit === 4 && ten >= 2) {
            groupText += "tư";
        } else if (unit === 5 && ten >= 1) {
            groupText += "lăm";
        } else if (unit > 0) {
            groupText += DIGITS[unit];
        }

        const unitIndex = i % 4;
        const unitName = UNITS[unitIndex];
        const billionsMultiplier = Math.floor(i / 4);
        let extraBillions = "";
        if (billionsMultiplier > 0 && unitIndex === 0) {
            extraBillions = "tỷ ".repeat(billionsMultiplier).trim();
        }

        let part = "";
        if (extraBillions) {
            part = `${groupText.trim()} ${extraBillions}`;
        } else if (unitName) {
            part = `${groupText.trim()} ${unitName}`;
        } else {
            part = groupText.trim();
        }

        words.push(part.trim());
    }

    return words.join(" ").trim() + " đồng";
}

// Chuyển đổi màn hình
function navigateTo(screenId) {
    document.querySelectorAll('.screen-view').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    const target = document.getElementById('screen' + screenId);
    if (target) target.classList.add('active');

    const navBtn = document.getElementById('nav' + screenId);
    if (navBtn) navBtn.classList.add('active');

    if (screenId === 'Cashier') renderPosQr();
}

// Định dạng tiền tệ VNĐ
function formatVnd(val) {
    return new Intl.NumberFormat('vi-VN').format(val) + " đ";
}

// Giả lập bắn thông báo biến động số dư
function simulatePush(bankCode, amount, desc, isDebit = false) {
    const banner = document.getElementById('phoneNotiBanner');
    const icon = document.getElementById('notiIcon');
    const title = document.getElementById('notiTitle');
    const text = document.getElementById('notiText');

    icon.innerText = bankCode;
    title.innerText = isDebit ? `Ngân hàng ${bankCode} (Trừ tiền)` : `Ngân hàng ${bankCode} (Cộng tiền)`;
    text.innerText = isDebit ? `TK: 0123456789 | GD: -${formatVnd(amount)} | ${desc}` : `TK: 0123456789 | GD: +${formatVnd(amount)} | ${desc}`;

    banner.classList.add('show');
    setTimeout(() => banner.classList.remove('show'), 3500);

    if (!isDebit) {
        state.todayRevenue += amount;
        state.txCount += 1;

        const tx = {
            id: Date.now(),
            bankCode: bankCode,
            amount: amount,
            desc: desc,
            time: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
            type: 'CREDIT'
        };
        state.transactions.unshift(tx);

        updateDashboard();
        notifyAudio(amount, bankCode);

        const qrScreenActive = document.getElementById('screenQrView').classList.contains('active');
        const cashierActive = document.getElementById('screenCashier').classList.contains('active');

        if (qrScreenActive || cashierActive) {
            showPaymentSuccess(amount, bankCode, state.currentOrder.orderCode);
        }
    } else {
        const tx = {
            id: Date.now(),
            bankCode: bankCode,
            amount: amount,
            desc: desc,
            time: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
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
    document.getElementById('homeTodayRevenue').innerText = formatVnd(state.todayRevenue);
    document.getElementById('homeTxCount').innerText = `${state.txCount} đơn`;
    document.getElementById('homeAvgValue').innerText = state.txCount > 0 ? formatVnd(Math.round(state.todayRevenue / state.txCount)) : "0 đ";

    const recentList = document.getElementById('homeRecentList');
    if (state.transactions.length === 0) {
        recentList.innerHTML = `<div class="empty-state">Chưa có giao dịch nào hôm nay</div>`;
    } else {
        recentList.innerHTML = state.transactions.slice(0, 4).map(tx => `
            <div class="tx-row ${tx.type === 'CREDIT' ? 'tx-credit' : 'tx-debit'}">
                <div class="tx-left">
                    <div class="tx-badge">${tx.bankCode}</div>
                    <div>
                        <div class="tx-desc">${tx.desc}</div>
                        <div class="tx-time">${tx.time}</div>
                    </div>
                </div>
                <div class="tx-amount">${tx.type === 'CREDIT' ? '+' : '-'}${formatVnd(tx.amount)}</div>
            </div>
        `).join('');
    }

    filterHistory('ALL');

    document.getElementById('statsTotalRevenue').innerText = formatVnd(state.todayRevenue);
    document.getElementById('statsTotalCount').innerText = `${state.txCount} lượt`;
    document.getElementById('statsAvgValue').innerText = state.txCount > 0 ? formatVnd(Math.round(state.todayRevenue / state.txCount)) : "0 đ";

    const bankMap = {};
    state.transactions.filter(t => t.type === 'CREDIT').forEach(t => {
        bankMap[t.bankCode] = (bankMap[t.bankCode] || 0) + t.amount;
    });

    const statsBankList = document.getElementById('statsBankList');
    if (Object.keys(bankMap).length === 0) {
        statsBankList.innerHTML = `<div class="empty-state">Chưa có dữ liệu ngân hàng</div>`;
    } else {
        statsBankList.innerHTML = Object.entries(bankMap).map(([bank, total]) => `
            <div class="tx-row">
                <div class="tx-left">
                    <div class="tx-badge">${bank}</div>
                    <div><strong>Ngân hàng ${bank}</strong></div>
                </div>
                <div class="tx-amount text-success">${formatVnd(total)}</div>
            </div>
        `).join('');
    }
}

function filterHistory(type) {
    const list = document.getElementById('historyFullList');
    const filtered = type === 'ALL' ? state.transactions : state.transactions.filter(t => t.type === type);
    if (filtered.length === 0) {
        list.innerHTML = `<div class="empty-state">Không có giao dịch nào</div>`;
    } else {
        list.innerHTML = filtered.map(tx => `
            <div class="tx-row ${tx.type === 'CREDIT' ? 'tx-credit' : 'tx-debit'}">
                <div class="tx-left">
                    <div class="tx-badge">${tx.bankCode}</div>
                    <div>
                        <div class="tx-desc">${tx.desc}</div>
                        <div class="tx-time">${tx.time}</div>
                    </div>
                </div>
                <div class="tx-amount">${tx.type === 'CREDIT' ? '+' : '-'}${formatVnd(tx.amount)}</div>
            </div>
        `).join('');
    }
}

// Bàn phím thu ngân
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

function posSubmit() {
    // Tự động sinh QR khi gõ
}

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

// Bàn phím tạo đơn
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
    if (amount <= 0) return alert("Vui lòng nhập số tiền hợp lệ!");

    state.currentOrder.amount = amount;
    state.currentOrder.desc = document.getElementById('createPayNote').value;
    state.currentOrder.orderCode = "TP" + Math.floor(1000 + Math.random() * 9000);

    document.getElementById('qrScreenAmount').innerText = formatVnd(amount);
    document.getElementById('qrScreenAcc').innerText = `${state.activeBank.name} • ${state.activeBank.accountNumber}`;
    document.getElementById('qrScreenName').innerText = state.activeBank.accountName;
    document.getElementById('qrScreenDesc').innerText = `Nội dung: ${state.currentOrder.orderCode}`;

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
        width: 200,
        height: 200,
        colorDark: "#000000",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.M
    });

    navigateTo('QrView');
}

function showPaymentSuccess(amount, bank, code) {
    document.getElementById('successAmount').innerText = formatVnd(amount);
    document.getElementById('successBank').innerText = bank;
    document.getElementById('successCode').innerText = code || "POS";
    navigateTo('Success');
}

function testSpeaker() {
    notifyAudio(350000, "MBBank");
}

function changeAudioMode() {
    state.audioMode = document.getElementById('webAudioMode').value;
}

// Khởi tạo đồng hồ & dữ liệu ban đầu
setInterval(() => {
    const now = new Date();
    const timeStr = now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    document.getElementById('phoneTime').innerText = timeStr;
}, 1000);

window.addEventListener('DOMContentLoaded', () => {
    initVietnameseVoices();
    updateDashboard();
    renderPosQr();
});
