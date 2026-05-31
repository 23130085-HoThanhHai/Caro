<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("activeNav", "home"); %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Gomoku Zen</title>
    <link rel="stylesheet" href="assets/css/app.css" />
</head>
<body>
<div class="page">
    <%@ include file="WEB-INF/jsp/common/header.jspf" %>

    <main class="main" role="main">
        <section class="hero" aria-label="Giới thiệu">
            <div class="container hero-inner">
                <div class="hero-copy">
                    <div class="badge">TRẢI NGHIỆM CHIẾN THUẬT ĐỈNH CAO</div>
                    <h1 class="hero-title">
                        Chinh Phục Đỉnh Cao
                        <span class="accent">Cờ Caro</span>
                    </h1>
                    <p class="hero-desc">
                        Gomoku Zen mang đến không gian thi đấu tĩnh lặng nhưng đầy kịch tính.
                        Nâng tầm kỹ năng của bạn qua từng nước cờ, từ những ván đấu thư giãn đến các trận xếp hạng căng thẳng.
                    </p>

                    <div class="hero-actions">
                        <a class="btn btn-primary btn-lg" href="#modes">Bắt Đầu Ngay</a>
                        <a class="btn btn-outline btn-lg" href="rules.jsp">Xem Luật Chơi</a>
                    </div>
                </div>

                <div class="hero-media" aria-label="Xem trước">
                    <div class="hero-image-card">
                        <img
                                class="hero-image"
                                src="assets/img/hero-board.svg"
                                alt="Bàn cờ Gomoku"
                                loading="lazy"
                                decoding="async"
                        />
                    </div>

                    <div class="hero-float" aria-label="Top tuần">
                        <div class="hero-float-icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M7 4h10v3a4 4 0 0 1-4 4h-2a4 4 0 0 1-4-4V4Z" stroke="currentColor" stroke-width="1.8" />
                                <path d="M9 21h6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                                <path d="M12 11v10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                                <path d="M7 6H5a2 2 0 0 0 2 2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                                <path d="M17 6h2a2 2 0 0 1-2 2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                            </svg>
                        </div>
                        <div class="hero-float-text">
                            <div class="hero-float-meta">TOP #1 TUẦN</div>
                            <div class="hero-float-name">Minh Triết</div>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <section class="section" id="modes" aria-label="Chế độ chơi">
            <div class="container section-head">
                <div>
                    <h2 class="section-title">Chọn Chế Độ Chơi</h2>
                    <p class="section-sub">Thử thách bản thân với nhiều hình thức thi đấu đa dạng.</p>
                </div>
                <div class="section-accent" aria-hidden="true"></div>
            </div>

            <div class="container mode-grid">
                <a class="card mode-card feature-card" href="create-room">
                    <div class="mode-icon mode-icon--peach" aria-hidden="true">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M12 5v14" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                            <path d="M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                            <rect x="4" y="4" width="16" height="16" rx="4" stroke="currentColor" stroke-width="1.6" opacity="0.6" />
                        </svg>
                    </div>
                    <h3 class="card-title">Tạo Phòng</h3>
                    <p class="card-desc">Tự tạo không gian thi đấu riêng và mời bạn bè tham gia.</p>
                    <span class="card-link">Bắt đầu <span aria-hidden="true">→</span></span>
                </a>

                <style>

                    .feature-card{
                        display: block;
                        text-decoration: none;
                        background: #ffffff;
                        border-radius: 16px;
                        padding: 24px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                        transition: all 0.3s ease;
                        color: #333;
                    }

                    .feature-card:hover{
                        transform: translateY(-5px);
                        box-shadow: 0 8px 20px rgba(0,0,0,0.15);
                    }

                    .card-title{
                        font-size: 24px;
                        margin-bottom: 12px;
                        color: #222;
                    }

                    .card-desc{
                        font-size: 15px;
                        line-height: 1.6;
                        margin-bottom: 20px;
                        color: #666;
                    }

                    .card-link{
                        font-weight: bold;
                        color: #3498db;
                        display: inline-flex;
                        align-items: center;
                        gap: 6px;
                    }

                </style>
                <!--4.1 Người chơi click “Tìm Phòng” trên giao diện Sảnh.-->
                <a href="find-room" class="feature-card">
                    <div class="mode-icon mode-icon--blue" aria-hidden="true">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <circle cx="11" cy="11" r="6" stroke="currentColor" stroke-width="2" />
                            <path d="M20 20l-3.5-3.5" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                        </svg>
                    </div>

                    <h3 class="card-title">
                        Tìm Phòng
                    </h3>

                    <p class="card-desc">
                        Dạo qua danh sách các phòng đang chờ đối thủ gia nhập.
                    </p>

                    <span class="card-link">
                  Tìm kiếm <span aria-hidden="true">→</span>
              </span>

                </a>


    </main>

    <%@ include file="WEB-INF/jsp/common/footer.jspf" %>
</div>
</body>
</html>
