/**
 * ==========================================================================
 * T45 MULTI-THREADING DASHBOARD - REAL-TIME ENGINE
 * Tự động cập nhật chỉ số mỗi 500ms & vẽ biểu đồ Canvas tốc độ 60fps
 * Không phụ thuộc thư viện ngoài (100% Native Vanilla JS)
 * ==========================================================================
 */

// Cấu hình cửa sổ dữ liệu trượt (Sliding Window Data Points)
const MAX_DATA_POINTS = 35;

const historyData = {
    timestamps: [],
    rps: [],
    latency: [],
    connections: [],
    threads: [],
    cpu: [],
    ram: []
};

let peakRps = 0.0;
let peakThreads = 0;

// Các canvas context
let ctxRps, ctxLatency, ctxThreads, ctxHardware;

document.addEventListener('DOMContentLoaded', () => {
    initCanvases();
    window.addEventListener('resize', handleResize);

    // Bắt đầu vòng lặp polling metrics mỗi 500ms
    fetchMetrics();
    setInterval(fetchMetrics, 500);
});

function initCanvases() {
    ctxRps = setupCanvas('canvasRps');
    ctxLatency = setupCanvas('canvasLatency');
    ctxThreads = setupCanvas('canvasThreads');
    ctxHardware = setupCanvas('canvasHardware');
}

function setupCanvas(canvasId) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return null;

    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.parentElement.getBoundingClientRect();

    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;

    const ctx = canvas.getContext('2d');
    ctx.scale(dpr, dpr);
    return ctx;
}

function handleResize() {
    initCanvases();
    renderAllCharts();
}

/**
 * Gửi yêu cầu HTTP GET tới /api/metrics để lấy toàn bộ dữ liệu trạng thái
 */
async function fetchMetrics() {
    try {
        const response = await fetch('/api/metrics');
        if (!response.ok) return;

        const data = await response.json();
        updateUI(data);
    } catch (err) {
        document.getElementById('modeNameDisplay').textContent = "Mất kết nối máy chủ...";
    }
}

/**
 * Cập nhật các thẻ KPI và mảng dữ liệu lịch sử
 */
function updateUI(data) {
    const { mode, server, system } = data;

    // 1. Cập nhật Badge Mode & Uptime
    if (mode) {
        document.getElementById('modeNameDisplay').textContent = `Mode ${mode.number}: ${mode.name}`;
    }
    if (server && server.uptimeSeconds !== undefined) {
        document.getElementById('uptimeDisplay').textContent = formatSeconds(server.uptimeSeconds);
    }

    // 2. Cập nhật KPI Cards
    if (server) {
        const rps = server.rps || 0.0;
        document.getElementById('kpiRps').textContent = rps.toFixed(1);
        if (rps > peakRps) {
            peakRps = rps;
            document.getElementById('kpiPeakRps').textContent = peakRps.toFixed(1);
        }

        document.getElementById('kpiTotalReqs').textContent = (server.totalRequests || 0).toLocaleString();
        document.getElementById('kpiSuccessReqs').textContent = (server.successfulRequests || 0).toLocaleString();
        document.getElementById('kpiFailedReqs').textContent = (server.failedRequests || 0).toLocaleString();

        const latency = server.latency || {};
        document.getElementById('kpiAvgLatency').textContent = (latency.avgMs || 0).toFixed(1);
        document.getElementById('kpiMinLatency').textContent = (latency.minMs || 0);
        document.getElementById('kpiP95Latency').textContent = (latency.p95Ms || 0).toFixed(1);

        document.getElementById('kpiActiveConns').textContent = server.activeConnections || 0;
    }

    if (system) {
        const liveThreads = system.liveThreadCount || 0;
        document.getElementById('kpiLiveThreads').textContent = liveThreads;
        if (liveThreads > peakThreads) {
            peakThreads = liveThreads;
            document.getElementById('kpiPeakThreads').textContent = peakThreads;
        }

        document.getElementById('kpiCpuUsage').textContent = (system.cpuProcessPercent || 0).toFixed(1);
        document.getElementById('kpiRamUsage').textContent = Math.round(system.heapUsedMb || 0);
        document.getElementById('kpiRamMax').textContent = Math.round(system.heapMaxMb || 0);
    }

    // 3. Đẩy dữ liệu vào mảng trượt (Sliding History)
    const timeLabel = new Date().toLocaleTimeString('vi-VN', { hour12: false });
    pushHistory(historyData.timestamps, timeLabel);
    pushHistory(historyData.rps, server ? server.rps : 0);
    pushHistory(historyData.latency, (server && server.latency) ? server.latency.avgMs : 0);
    pushHistory(historyData.connections, server ? server.activeConnections : 0);
    pushHistory(historyData.threads, system ? system.liveThreadCount : 0);
    pushHistory(historyData.cpu, system ? system.cpuProcessPercent : 0);
    pushHistory(historyData.ram, system ? system.heapUsedMb : 0);

    // 4. Cập nhật Legends
    if (server) {
        document.getElementById('legendRps').textContent = `${(server.rps || 0).toFixed(1)} RPS`;
        document.getElementById('legendLatency').textContent = `${((server.latency && server.latency.avgMs) || 0).toFixed(1)} ms`;
        document.getElementById('legendThreads').textContent = `Conns: ${server.activeConnections || 0} | Threads: ${system ? system.liveThreadCount : 0}`;
    }
    if (system) {
        document.getElementById('legendHardware').textContent = `CPU: ${(system.cpuProcessPercent || 0).toFixed(1)}% | RAM: ${Math.round(system.heapUsedMb || 0)}MB`;
    }

    // 5. Vẽ lại tất cả biểu đồ Canvas
    renderAllCharts();
}

function pushHistory(array, value) {
    array.push(value);
    if (array.length > MAX_DATA_POINTS) {
        array.shift();
    }
}

/**
 * Vẽ toàn bộ 4 biểu đồ Canvas
 */
function renderAllCharts() {
    drawAreaChart(ctxRps, 'canvasRps', historyData.rps, '#06b6d4', 'rgba(6, 182, 212, 0.25)', 'RPS');
    drawAreaChart(ctxLatency, 'canvasLatency', historyData.latency, '#8b5cf6', 'rgba(139, 92, 246, 0.25)', 'ms');
    drawDualLineChart(ctxThreads, 'canvasThreads', historyData.connections, historyData.threads, '#10b981', '#f43f5e');
    drawDualLineChart(ctxHardware, 'canvasHardware', historyData.cpu, historyData.ram, '#3b82f6', '#f59e0b');
}

/**
 * Hàm vẽ biểu đồ diện tích (Area Chart) với đường lưới và hiệu ứng Gradient
 */
function drawAreaChart(ctx, canvasId, data, strokeColor, fillColor, unit) {
    if (!ctx) return;
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    const w = canvas.parentElement.getBoundingClientRect().width;
    const h = canvas.parentElement.getBoundingClientRect().height;

    ctx.clearRect(0, 0, w, h);
    drawGrid(ctx, w, h);

    if (data.length < 2) return;

    const maxVal = Math.max(...data, 10);
    const stepX = w / (MAX_DATA_POINTS - 1);
    const startOffset = MAX_DATA_POINTS - data.length;

    ctx.beginPath();
    for (let i = 0; i < data.length; i++) {
        const x = (startOffset + i) * stepX;
        const y = h - (data[i] / maxVal) * (h - 25) - 10;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
    }

    // Tạo gradient fill
    const fillPath = new Path2D(ctx);
    fillPath.lineTo(w, h);
    fillPath.lineTo((startOffset) * stepX, h);
    fillPath.closePath();

    const gradient = ctx.createLinearGradient(0, 0, 0, h);
    gradient.addColorStop(0, fillColor);
    gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');
    ctx.fillStyle = gradient;
    ctx.fill(fillPath);

    // Vẽ nét viền rực rỡ
    ctx.strokeStyle = strokeColor;
    ctx.lineWidth = 2.5;
    ctx.shadowColor = strokeColor;
    ctx.shadowBlur = 8;
    ctx.stroke();
    ctx.shadowBlur = 0; // Reset shadow
}

/**
 * Hàm vẽ biểu đồ so sánh 2 đại lượng đối đầu (Dual Line Chart)
 */
function drawDualLineChart(ctx, canvasId, data1, data2, color1, color2) {
    if (!ctx) return;
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    const w = canvas.parentElement.getBoundingClientRect().width;
    const h = canvas.parentElement.getBoundingClientRect().height;

    ctx.clearRect(0, 0, w, h);
    drawGrid(ctx, w, h);

    if (data1.length < 2 && data2.length < 2) return;

    const maxVal = Math.max(...data1, ...data2, 10);
    const stepX = w / (MAX_DATA_POINTS - 1);
    const startOffset = MAX_DATA_POINTS - Math.max(data1.length, data2.length);

    // Đường 1
    if (data1.length >= 2) {
        ctx.beginPath();
        for (let i = 0; i < data1.length; i++) {
            const x = (startOffset + i) * stepX;
            const y = h - (data1[i] / maxVal) * (h - 25) - 10;
            if (i === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        }
        ctx.strokeStyle = color1;
        ctx.lineWidth = 2.2;
        ctx.shadowColor = color1;
        ctx.shadowBlur = 6;
        ctx.stroke();
        ctx.shadowBlur = 0;
    }

    // Đường 2
    if (data2.length >= 2) {
        ctx.beginPath();
        for (let i = 0; i < data2.length; i++) {
            const x = (startOffset + i) * stepX;
            const y = h - (data2[i] / maxVal) * (h - 25) - 10;
            if (i === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        }
        ctx.strokeStyle = color2;
        ctx.lineWidth = 2.2;
        ctx.shadowColor = color2;
        ctx.shadowBlur = 6;
        ctx.stroke();
        ctx.shadowBlur = 0;
    }
}

function drawGrid(ctx, w, h) {
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.04)';
    ctx.lineWidth = 1;

    // Đường ngang
    for (let y = 15; y < h; y += 35) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(w, y);
        ctx.stroke();
    }
}

// ==========================================================================
// TEST TRIGGER CONTROLS (DEMO CENTER)
// ==========================================================================

async function triggerRequest(endpoint, count = 1) {
    logToConsole(`[*] Đang gửi ${count} request tới ${endpoint}...`, 'info');
    const start = Date.now();

    try {
        const res = await fetch(endpoint);
        const data = await res.json();
        const latency = Date.now() - start;

        const threadInfo = data.thread ? `[Thread: ${data.thread}]` : '';
        const loomInfo = data.isVirtual ? `[Loom Virtual]` : '';
        logToConsole(`[✓] HTTP ${res.status} trong ${latency}ms ${threadInfo} ${loomInfo}`, 'success');
    } catch (err) {
        logToConsole(`[✗] Lỗi kết nối: ${err.message}`, 'error');
    }
}

async function triggerBurst(endpoint, count = 30) {
    logToConsole(`[🚀 BẮN TẢI ĐỒNG THỜI] Phát sinh ${count} requests song song vào ${endpoint}...`, 'warn');
    document.getElementById('consoleStatus').textContent = `Đang bắn ${count} requests...`;

    const start = Date.now();
    const promises = [];

    for (let i = 0; i < count; i++) {
        promises.push(
            fetch(endpoint)
                .then(r => r.json().then(d => ({ status: r.status, data: d })))
                .catch(e => ({ status: 0, error: e.message }))
        );
    }

    try {
        const results = await Promise.all(promises);
        const totalTime = Date.now() - start;
        const successCount = results.filter(r => r.status === 200).length;
        const failCount = count - successCount;

        logToConsole(`[🔥 HOÀN TẤT TẢI] ${count} reqs trong ${totalTime}ms (Thành công: ${successCount}, Lỗi: ${failCount})`, successCount === count ? 'success' : 'warn');
        document.getElementById('consoleStatus').textContent = `Hoàn tất`;
    } catch (e) {
        logToConsole(`[✗] Lỗi bắn tải: ${e.message}`, 'error');
    }
}

function logToConsole(message, type = 'info') {
    const body = document.getElementById('consoleBody');
    if (!body) return;

    const line = document.createElement('div');
    line.className = `log-line ${type}`;
    line.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;

    body.insertBefore(line, body.firstChild);
    while (body.children.length > 50) {
        body.removeChild(body.lastChild);
    }
}

function clearLogConsole() {
    const body = document.getElementById('consoleBody');
    if (body) body.innerHTML = '<div class="log-line info">[*] Nhật ký đã được làm sạch.</div>';
}

function formatSeconds(secs) {
    const h = Math.floor(secs / 3600).toString().padStart(2, '0');
    const m = Math.floor((secs % 3600) / 60).toString().padStart(2, '0');
    const s = Math.floor(secs % 60).toString().padStart(2, '0');
    return `${h}:${m}:${s}`;
}
