#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
 T45 MULTI-THREADING BENCHMARK TOOL (PYTHON ALTERNATIVE)
 Đề tài: Multi-threading Patterns in Network Programming - PTIT
 GVHD: TS. Đặng Ngọc Hùng
=============================================================================
"""

import argparse
import csv
import datetime
import os
import sys
import time
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor, as_completed

def run_benchmark(target_url, concurrency, total_requests, mode_name, csv_output):
    print("=" * 75)
    print(" 🚀 [T45 BENCHMARK TOOL] PYTHON CONCURRENT LOAD GENERATOR")
    print("=" * 75)
    print(f" 🎯 Mục tiêu URL:         {target_url}")
    print(f" 👥 Kết nối đồng thời:    {concurrency} concurrent threads")
    print(f" 📦 Tổng số requests:     {total_requests} requests")
    print(f" 🏷️ Mô hình kiểm thử:     {mode_name}")
    print("-" * 75)
    print(" [*] Đang phát sinh tải đồng thời...")

    latencies_ms = []
    success_count = 0
    fail_count = 0

    def make_request():
        start = time.perf_counter()
        try:
            req = urllib.request.Request(
                target_url,
                headers={"User-Agent": "T45-PythonLoadTester/1.0"}
            )
            with urllib.request.urlopen(req, timeout=15) as res:
                elapsed_ms = (time.perf_counter() - start) * 1000.0
                return True, elapsed_ms, res.status
        except urllib.error.HTTPError as e:
            elapsed_ms = (time.perf_counter() - start) * 1000.0
            return (e.code < 400), elapsed_ms, e.code
        except Exception as e:
            elapsed_ms = (time.perf_counter() - start) * 1000.0
            return False, elapsed_ms, 0

    bench_start = time.perf_counter()

    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(make_request) for _ in range(total_requests)]
        for f in as_completed(futures):
            ok, lat_ms, status = f.result()
            latencies_ms.append(lat_ms)
            if ok:
                success_count += 1
            else:
                fail_count += 1

    total_time_sec = time.perf_counter() - bench_start
    rps = total_requests / total_time_sec if total_time_sec > 0 else 0

    latencies_ms.sort()
    min_ms = latencies_ms[0] if latencies_ms else 0
    max_ms = latencies_ms[-1] if latencies_ms else 0
    avg_ms = sum(latencies_ms) / len(latencies_ms) if latencies_ms else 0

    def get_percentile(data, pct):
        if not data:
            return 0
        idx = int(pct * len(data)) - 1
        return data[max(0, min(idx, len(data) - 1))]

    p50_ms = get_percentile(latencies_ms, 0.50)
    p95_ms = get_percentile(latencies_ms, 0.95)
    p99_ms = get_percentile(latencies_ms, 0.99)

    print("\n" + "=" * 75)
    print(" 📊 KẾT QUẢ ĐO ĐẠC HIỆU NĂNG (BENCHMARK RESULTS)")
    print("=" * 75)
    print(f" ⏱️ Tổng thời gian chạy:   {total_time_sec:.2f} giây")
    print(f" ⚡ Thông lượng (Throughput): {rps:.2f} Requests/giây (RPS)")
    print("-" * 75)
    print(f" ✅ Requests thành công:    {success_count} / {total_requests} ({(success_count/total_requests)*100:.1f}%)")
    print(f" ❌ Requests thất bại/lỗi:  {fail_count} ({(fail_count/total_requests)*100:.1f}%)")
    print("-" * 75)
    print(" 📈 Phân bố độ trễ mạng (Latency Distribution):")
    print(f"    - Min Latency:          {min_ms:.1f} ms")
    print(f"    - Avg Latency:          {avg_ms:.2f} ms")
    print(f"    - Median (P50):         {p50_ms:.1f} ms")
    print(f"    - 95th Percentile (P95):{p95_ms:.1f} ms")
    print(f"    - 99th Percentile (P99):{p99_ms:.1f} ms")
    print(f"    - Max Latency:          {max_ms:.1f} ms")
    print("=" * 75 + "\n")

    if csv_output:
        os.makedirs(os.path.dirname(csv_output) or ".", exist_ok=True)
        is_new = not os.path.exists(csv_output)
        with open(csv_output, "a", newline="", encoding="utf-8") as f:
            writer = csv.writer(f)
            if is_new:
                writer.writerow([
                    "Timestamp", "Mode", "TargetUrl", "Concurrency", "TotalReqs",
                    "SuccessReqs", "FailedReqs", "TotalTimeSec", "RPS",
                    "MinLatencyMs", "AvgLatencyMs", "P50Ms", "P95Ms", "P99Ms", "MaxLatencyMs"
                ])
            now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            writer.writerow([
                now_str, mode_name, target_url, concurrency, total_requests,
                success_count, fail_count, round(total_time_sec, 2), round(rps, 2),
                round(min_ms, 1), round(avg_ms, 2), round(p50_ms, 1),
                round(p95_ms, 1), round(p99_ms, 1), round(max_ms, 1)
            ])
        print(f"[*] Đã lưu kết quả đối sánh vào file: {csv_output}")

def main():
    parser = argparse.ArgumentParser(description="T45 Multi-Threading Load Tester")
    parser.add_argument("--url", default="http://localhost:8080/api/delay?ms=100", help="Target URL")
    parser.add_argument("-c", "--concurrency", type=int, default=100, help="Concurrent clients")
    parser.add_argument("-n", "--requests", type=int, default=1000, help="Total requests")
    parser.add_argument("-m", "--mode", default="Python Client", help="Server mode name")
    parser.add_argument("-o", "--csv", default="benchmark/benchmark_results.csv", help="CSV Output path")

    args = parser.parse_args()
    run_benchmark(args.url, args.concurrency, args.requests, args.mode, args.csv)

if __name__ == "__main__":
    main()
