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

    // Tải dữ liệu đối sánh Benchmark
    fetchBenchmarkData();
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

        const queueBadge = document.getElementById('queueBadgeDisplay');
        const queueText = document.getElementById('queueTextDisplay');
        if (queueBadge && queueText) {
            if (mode.number === 3 || mode.number === 5) {
                queueBadge.style.display = 'inline-flex';
                queueBadge.style.background = 'rgba(245, 158, 11, 0.12)';
                queueBadge.style.borderColor = 'rgba(245, 158, 11, 0.35)';
                queueBadge.style.color = '#fbbf24';
                queueText.textContent = `Queue: ${mode.queueSize || 0}/${mode.queueCapacity || 1000} | Workers: ${mode.activeWorkers || 0}/16`;
            } else if (mode.number === 4) {
                queueBadge.style.display = 'inline-flex';
                queueBadge.style.background = 'rgba(16, 185, 129, 0.12)';
                queueBadge.style.borderColor = 'rgba(16, 185, 129, 0.35)';
                queueBadge.style.color = '#34d399';
                const liveTh = system ? system.liveThreadCount : 16;
                queueBadge.innerHTML = `<span class="queue-icon">🧵</span><span>OS Carrier Threads: <b>${liveTh}</b> (M:N Mapping)</span>`;
            } else if (mode.number === 2) {
                queueBadge.style.display = 'inline-flex';
                queueBadge.style.background = 'rgba(244, 63, 94, 0.12)';
                queueBadge.style.borderColor = 'rgba(244, 63, 94, 0.35)';
                queueBadge.style.color = '#fb7185';
                queueBadge.innerHTML = `<span class="queue-icon">⚠️</span><span>1 Thread Per Socket (1MB Stack / Luồng)</span>`;
            } else {
                queueBadge.style.display = 'inline-flex';
                queueBadge.style.background = 'rgba(100, 116, 139, 0.15)';
                queueBadge.style.borderColor = 'rgba(100, 116, 139, 0.35)';
                queueBadge.style.color = '#94a3b8';
                queueBadge.innerHTML = `<span class="queue-icon">⏸️</span><span>Đơn luồng tuần tự (Main Thread duy nhất)</span>`;
            }
        }
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

async function triggerBackendBenchmark(concurrency = 500, requests = 2000) {
    logToConsole(`[🚀 KÍCH HOẠT BENCHMARK TIẾN TRÌNH ĐỘC LẬP] Đang phát động tải ${concurrency} Clients đồng thời (${requests} reqs)...`, 'warn');
    document.getElementById('consoleStatus').textContent = `Đang chạy Benchmark ${concurrency} clients...`;

    try {
        const res = await fetch(`/api/benchmark/trigger?c=${concurrency}&n=${requests}`);
        const data = await res.json();
        logToConsole(`[✓] Đã khởi chạy tiến trình JavaLoadTester: ${data.concurrency} clients đang bắn tải TCP! Quan sát Biểu đồ số 3...`, 'success');

        // Tự động làm mới bảng benchmark sau khi tiến trình độc lập chạy xong
        setTimeout(() => {
            fetchBenchmarkData();
            logToConsole(`[✓] Đợt tải đã hoàn tất! Bảng đối sánh Benchmark đã được cập nhật dữ liệu đo đạc mới.`, 'info');
            document.getElementById('consoleStatus').textContent = 'Sẵn sàng';
        }, 15000);
    } catch (e) {
        logToConsole(`[✗] Lỗi kích hoạt benchmark: ${e.message}`, 'error');
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

// ==========================================================================
// BENCHMARK DATA FETCH & VISUALIZATION ENGINE
// ==========================================================================

let benchmarkHistoryList = [];
let historySortField = 'timestamp';
let historySortAsc = false; // Mặc định: false = Mới nhất trước (Descending)

async function fetchBenchmarkData() {
    try {
        const res = await fetch('/api/benchmark');
        if (!res.ok) return;
        const data = await res.json();

        if (data.summary && data.summary.length > 0) {
            renderBenchmarkMatrix(data.summary);
            renderBenchmarkBars(data.summary);
        }

        if (data.history) {
            benchmarkHistoryList = data.history;
            applySortAndRenderHistory();
        }
    } catch (e) {
        console.error("Lỗi lấy dữ liệu benchmark:", e);
    }
}

/**
 * Đảo chiều sắp xếp thời gian (Mới nhất <-> Cũ nhất)
 */
function toggleTimeSort() {
    if (historySortField === 'timestamp') {
        historySortAsc = !historySortAsc;
    } else {
        historySortField = 'timestamp';
        historySortAsc = false;
    }
    applySortAndRenderHistory();
}

/**
 * Sắp xếp theo một trường bất kỳ khi nhấp vào tiêu đề cột
 */
function sortHistory(field) {
    if (historySortField === field) {
        historySortAsc = !historySortAsc;
    } else {
        historySortField = field;
        historySortAsc = (field === 'timestamp') ? false : true;
    }
    applySortAndRenderHistory();
}

/**
 * Thực hiện sắp xếp mảng dữ liệu và cập nhật giao diện
 */
function applySortAndRenderHistory() {
    if (!benchmarkHistoryList || benchmarkHistoryList.length === 0) {
        renderBenchmarkHistory([]);
        return;
    }

    const sorted = [...benchmarkHistoryList].sort((a, b) => {
        let valA = a[historySortField];
        let valB = b[historySortField];

        if (historySortField === 'timestamp') {
            const timeA = new Date(valA).getTime() || valA;
            const timeB = new Date(valB).getTime() || valB;
            return historySortAsc ? (timeA > timeB ? 1 : -1) : (timeA < timeB ? 1 : -1);
        }

        const numA = parseFloat(valA);
        const numB = parseFloat(valB);
        if (!isNaN(numA) && !isNaN(numB)) {
            return historySortAsc ? numA - numB : numB - numA;
        }

        const strA = (valA || '').toString();
        const strB = (valB || '').toString();
        return historySortAsc ? strA.localeCompare(strB) : strB.localeCompare(strA);
    });

    updateSortIndicators();
    renderBenchmarkHistory(sorted);
}

function updateSortIndicators() {
    const arrow = historySortAsc ? '▲' : '▼';
    const fields = ['timestamp', 'concurrency', 'totalReqs', 'totalTimeSec', 'rps', 'avgLatency', 'p95'];
    fields.forEach(f => {
        const el = document.getElementById(`sortIcon_${f}`);
        if (el) {
            el.textContent = (historySortField === f) ? arrow : '↕';
            el.style.opacity = (historySortField === f) ? '1' : '0.35';
        }
    });

    const toggleBtnText = document.getElementById('sortToggleText');
    if (toggleBtnText) {
        if (historySortField === 'timestamp') {
            toggleBtnText.textContent = historySortAsc ? 'Cũ nhất trước ▲' : 'Mới nhất trước ▼';
        } else {
            toggleBtnText.textContent = `Sort: ${historySortField} ${arrow}`;
        }
    }
}

function renderBenchmarkMatrix(summaryList) {
    const tbody = document.getElementById('benchmarkMatrixBody');
    if (!tbody) return;

    tbody.innerHTML = summaryList.map(item => {
        const isLoom = item.mode === 4;
        const rowClass = isLoom ? 'highlight-loom' : '';
        const tagColor = item.mode === 4 ? 'tag-emerald' : (item.mode === 3 ? 'tag-amber' : (item.mode === 5 ? 'tag-violet' : (item.mode === 2 ? 'tag-rose' : 'tag-cyan')));

        return `
            <tr class="${rowClass}">
                <td><b class="${isLoom ? 'text-success' : ''}">${item.title}</b></td>
                <td><span class="badge-tag ${tagColor}">${item.category}</span></td>
                <td><span class="mono" style="font-weight:700; color:${isLoom ? '#34d399' : '#06b6d4'}">${item.rps.toFixed(1)} req/s</span></td>
                <td><span class="mono" style="font-weight:700; color:${item.p95Latency > 1000 ? '#f87171' : '#a78bfa'}">${item.p95Latency} ms</span></td>
                <td><span class="mono">${item.threads}</span></td>
                <td><span class="mono">${item.ramMb} MB</span></td>
                <td><span class="mono">${item.safeConns.toLocaleString()} conns</span></td>
                <td style="color:#94a3b8; font-size:12px;">${item.bottleneck}</td>
                <td><span class="badge-tag ${isLoom ? 'tag-emerald' : (item.mode === 5 ? 'tag-violet' : 'tag-amber')}">${item.status}</span></td>
            </tr>
        `;
    }).join('');
}

function renderBenchmarkBars(summaryList) {
    const rpsContainer = document.getElementById('rpsBarsContainer');
    const latencyContainer = document.getElementById('latencyBarsContainer');
    if (!rpsContainer || !latencyContainer) return;

    // 1. Throughput RPS Bars
    const maxRps = Math.max(...summaryList.map(s => s.rps), 100);
    rpsContainer.innerHTML = summaryList.map(item => {
        const pct = Math.min(100, Math.max(3, (item.rps / maxRps) * 100));
        const colorClass = item.mode === 4 ? 'emerald' : (item.mode === 2 ? 'cyan' : (item.mode === 3 ? 'amber' : (item.mode === 5 ? 'violet' : 'rose')));
        return `
            <div class="bar-row">
                <div class="bar-row-info">
                    <span>${item.title.split(':')[0]} (${item.category})</span>
                    <span class="mono" style="font-weight:700;">${item.rps.toFixed(1)} RPS</span>
                </div>
                <div class="bar-track">
                    <div class="bar-fill ${colorClass}" style="width: ${pct}%;"></div>
                </div>
            </div>
        `;
    }).join('');

    // 2. Latency P95 Bars (Càng thấp càng tốt, vẽ tỉ lệ nghịch hoặc vẽ trực tiếp)
    const maxLatency = Math.max(...summaryList.map(s => s.p95Latency), 100);
    latencyContainer.innerHTML = summaryList.map(item => {
        const pct = Math.min(100, Math.max(3, (item.p95Latency / maxLatency) * 100));
        const colorClass = item.p95Latency > 1000 ? 'rose' : (item.mode === 4 ? 'emerald' : 'violet');
        return `
            <div class="bar-row">
                <div class="bar-row-info">
                    <span>${item.title.split(':')[0]} (${item.category})</span>
                    <span class="mono" style="font-weight:700;">${item.p95Latency} ms</span>
                </div>
                <div class="bar-track">
                    <div class="bar-fill ${colorClass}" style="width: ${pct}%;"></div>
                </div>
            </div>
        `;
    }).join('');
}

function renderBenchmarkHistory(historyList) {
    const tbody = document.getElementById('csvHistoryBody');
    const countBadge = document.getElementById('historyCountBadge');
    if (!tbody) return;

    if (countBadge) {
        countBadge.textContent = `${historyList.length} bản ghi test`;
    }

    if (historyList.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; color:#64748b; padding:20px;">Chưa có dữ liệu trong benchmark_results.csv</td></tr>`;
        return;
    }

    tbody.innerHTML = historyList.map(row => {
        const rps = parseFloat(row.rps) || 0;
        const avg = parseFloat(row.avgLatency) || 0;
        const p95 = parseFloat(row.p95) || 0;
        const isLoom = row.mode && row.mode.includes("Mode 4");

        return `
            <tr class="${isLoom ? 'highlight-loom' : ''}">
                <td class="mono" style="font-size:11px; color:#94a3b8;">${row.timestamp}</td>
                <td><b>${row.mode}</b></td>
                <td class="mono">${row.concurrency}</td>
                <td class="mono">${row.totalReqs}</td>
                <td class="mono text-success">${row.successReqs}</td>
                <td class="mono">${row.totalTimeSec}s</td>
                <td class="mono" style="font-weight:700; color:${isLoom ? '#34d399' : '#06b6d4'};">${rps.toFixed(2)}</td>
                <td class="mono">${avg.toFixed(1)}ms</td>
                <td class="mono" style="font-weight:700; color:${p95 > 1000 ? '#f87171' : '#a78bfa'};">${p95}ms</td>
            </tr>
        `;
    }).join('');
}


