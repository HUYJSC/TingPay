// =========================================================
// TingPay – Web Interactive Engine & Simulator
// =========================================================

// State Management
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

// CRC16 CCITT-FALSE for VietQR
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

// Generate VietQR EMVCo Payload
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

// Vietnamese Number to Words
function numberToVietnameseWords(n) {
    if (n === 0) return "không đồng";
    const digits = ["không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"];
    const units = ["", "nghìn", "triệu", "tỷ"];

    let num = n;
    let groups = [];
    while (num > 0) {
        groups.push(num % 1000);
        num = Math.floor(num / 1000);
    }

    let words = [];
    for (let i = groups.length - 1; i >= 0; i--) {
        let val = groups[i];
        if (val === 0) continue;

        let hundred = Math.floor(val / 100);
        let ten = Math.floor((val % 100) / 10);
        let unit = val % 10;
        let part = "";

        if (hundred > 0 || i < groups.length - 1) part += digits[hundred] + " trăm ";
        if (ten > 1) {
            part += digits[ten] + " mươi ";
            if (unit === 1) part += "mốt";
            else if (unit === 5) part += "lăm";
            else if (unit > 0) part += digits[unit];
        } else if (ten === 1) {
            part += "mười ";
            if (unit === 5) part += "lăm";
            else if (unit > 0) part += digits[unit];
        } else if (ten === 0 && unit > 0) {
            if (hundred > 0 || i < groups.length - 1) part += "linh ";
            part += digits[unit];
        }

        let unitText = units[i % units.length];
        words.push((part.trim() + " " + unitText).trim());
    }

    return words.join(" ") + " đồng";
}

// Audio Engine: Synthesizes real "Ting" chime & Speech
function playTingSound() {
    try {
        if (!state.audioContext) {
            state.audioContext = new (window.AudioContext || window.webkitAudioContext)();
        }
        const ctx = state.audioContext;
        if (ctx.state === 'suspended') ctx.resume();

        // Bell chime fundamental + harmonic frequencies
        const osc1 = ctx.createOscillator();
        const osc2 = ctx.createOscillator();
        const gain = ctx.createGain();

        osc1.type = 'sine';
        osc1.frequency.setValueAtTime(1760, ctx.currentTime); // A6 Note
        osc1.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.6);

        osc2.type = 'triangle';
        osc2.frequency.setValueAtTime(2637, ctx.currentTime); // E7 Note
        osc2.frequency.exponentialRampToValueAtTime(1318, ctx.currentTime + 0.6);

        gain.gain.setValueAtTime(0.7, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.6);

        osc1.connect(gain);
        osc2.connect(gain);
        gain.connect(ctx.destination);

        osc1.start();
        osc2.start();
        osc1.stop(ctx.currentTime + 0.6);
        osc2.stop(ctx.currentTime + 0.6);
    } catch (e) {
        console.warn("Web Audio API error", e);
    }
}

function speakText(text) {
    if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
        const utter = new SpeechSynthesisUtterance(text);
        utter.lang = 'vi-VN';
        utter.rate = 1.0;
        window.speechSynthesis.speak(utter);
    }
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

// Navigation & Screen Switcher
function navigateTo(screenId) {
    document.querySelectorAll('.screen-view').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    const target = document.getElementById('screen' + screenId);
    if (target) target.classList.add('active');

    const navBtn = document.getElementById('nav' + screenId);
    if (navBtn) navBtn.classList.add('active');

    // Auto refresh QR if navigating to POS
    if (screenId === 'Cashier') renderPosQr();
}

// Format Currency
function formatVnd(val) {
    return new Intl.NumberFormat('vi-VN').format(val) + " đ";
}

// Simulate Push Notification
function simulatePush(bankCode, amount, desc, isDebit = false) {
    const banner = document.getElementById('phoneNotiBanner');
    const icon = document.getElementById('notiIcon');
    const title = document.getElementById('notiTitle');
    const text = document.getElementById('notiText');

    icon.innerText = bankCode;
    title.innerText = isDebit ? `Ngân hàng ${bankCode} (-)` : `Ngân hàng ${bankCode} (+)`;
    text.innerText = isDebit ? `TK: 0123456789 | GD: -${formatVnd(amount)} | ${desc}` : `TK: 0123456789 | GD: +${formatVnd(amount)} | ${desc}`;

    banner.classList.add('show');
    setTimeout(() => banner.classList.remove('show'), 3500);

    if (!isDebit) {
        // Process Credit Transaction
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

        // Update Dashboard
        updateDashboard();

        // Audio & TTS
        notifyAudio(amount, bankCode);

        // Check if QR payment screen is open -> Transition to Success Celebration
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

    // Update Recent List on Home
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

    // Update Full History
    filterHistory('ALL');

    // Update Stats
    document.getElementById('statsTotalRevenue').innerText = formatVnd(state.todayRevenue);
    document.getElementById('statsTotalCount').innerText = `${state.txCount} lượt`;
    document.getElementById('statsAvgValue').innerText = state.txCount > 0 ? formatVnd(Math.round(state.todayRevenue / state.txCount)) : "0 đ";
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

// POS Cashier Keypad Logic
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
    // Already dynamic on typing
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

// Create Payment Keypad Logic
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

// Initialize clock & default data
setInterval(() => {
    const now = new Date();
    const timeStr = now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    document.getElementById('phoneTime').innerText = timeStr;
}, 1000);

window.addEventListener('DOMContentLoaded', () => {
    updateDashboard();
    renderPosQr();
});
