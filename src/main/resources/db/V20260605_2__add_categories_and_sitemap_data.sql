-- Create categories table and seed it with sitemap categories
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(280) UNIQUE NOT NULL,
    parent_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_categories_parent_id(parent_id),
    INDEX idx_categories_is_active(is_active)
);

-- Seed Categories (Parent categories)
INSERT INTO categories (id, name, slug, parent_id, sort_order, is_active) VALUES
(1, 'Trang phục nữ & Đồ lót', 'womenswear-underwear', NULL, 1, TRUE),
(2, 'Điện thoại & Đồ điện tử', 'phones-electronics', NULL, 2, TRUE),
(3, 'Phụ kiện thời trang', 'fashion-accessories', NULL, 3, TRUE),
(4, 'Trang phục nam & Đồ lót', 'menswear-underwear', NULL, 4, TRUE),
(5, 'Đồ gia dụng', 'home-supplies', NULL, 5, TRUE),
(6, 'Chăm sóc sắc đẹp & Chăm sóc cá nhân', 'beauty-personal-care', NULL, 6, TRUE),
(7, 'Giày', 'shoes', NULL, 7, TRUE),
(8, 'Thể thao & Ngoài trời', 'sports-outdoor', NULL, 8, TRUE),
(9, 'Hành lý & Túi xách', 'luggage-bags', NULL, 9, TRUE),
(10, 'Đồ chơi & sở thích', 'toys-hobbies', NULL, 10, TRUE),
(11, 'Ô tô & xe máy', 'automotive-motorcycle', NULL, 11, TRUE),
(12, 'Thời trang trẻ em', 'kids-fashion', NULL, 12, TRUE),
(13, 'Đồ dùng nhà bếp', 'kitchenware', NULL, 13, TRUE),
(14, 'Thiết bị gia dụng', 'household-appliances', NULL, 14, TRUE),
(15, 'Đồ ăn & Đồ uống', 'food-beverages', NULL, 15, TRUE),
(16, 'Sức khỏe', 'health', NULL, 16, TRUE),
(17, 'Đồ nội thất', 'furniture', NULL, 17, TRUE),
(18, 'Đồ dùng cho thú cưng', 'pet-supplies', NULL, 18, TRUE),
(19, 'Sách, tạp chí & âm thanh', 'books-magazines-audio', NULL, 19, TRUE),
(20, 'Phụ kiện trang sức & Phái sinh', 'jewelry-accessories-derivatives', NULL, 20, TRUE);

-- Seed Subcategories (womenswear-underwear children)
INSERT INTO categories (name, slug, parent_id, sort_order, is_active) VALUES
('Đồ lót nữ', 'women-s-underwear', 1, 1, TRUE),
('Bộ vét và quần yếm nữ', 'women-s-suits-sets', 1, 2, TRUE),
('Váy nữ', 'women-s-dresses', 1, 3, TRUE),
('Áo nữ', 'women-s-tops', 1, 4, TRUE),
('Đồ ngủ & đồ mặc nhà cho nữ', 'women-s-sleepwear-loungewear', 1, 5, TRUE),
('Quần nữ', 'women-s-bottoms', 1, 6, TRUE),
('Trang phục đặc biệt dành cho nữ', 'women-s-special-clothing', 1, 7, TRUE);

-- Seed Subcategories (phones-electronics children)
INSERT INTO categories (name, slug, parent_id, sort_order, is_active) VALUES
('Phụ kiện điện thoại', 'mobile-phone-accessories', 2, 1, TRUE),
('Âm thanh & Video', 'audio-video', 2, 2, TRUE),
('Thiết bị thông minh & Thiết bị đeo', 'smart-wearable-devices', 2, 3, TRUE),
('Camera & Nhiếp ảnh', 'cameras-photography', 2, 4, TRUE),
('Phụ kiện đa năng', 'universal-accessories', 2, 5, TRUE),
('Chơi game & Bảng điều khiển', 'gaming-consoles', 2, 6, TRUE),
('Điện thoại & Máy tính bảng', 'phones-tablets', 2, 7, TRUE),
('Phụ kiện máy tính bảng & máy tính', 'tablet-computer-accessories', 2, 8, TRUE),
('Thiết bị giáo dục', 'education-devices', 2, 9, TRUE);

-- Seed Subcategories (fashion-accessories children)
INSERT INTO categories (name, slug, parent_id, sort_order, is_active) VALUES
('Kính mắt', 'eyewear', 3, 1, TRUE),
('Phụ kiện cài đầu', 'hair-accessories', 3, 2, TRUE),
('Phục sức & phụ kiện', 'costume-jewelry-accessories', 3, 3, TRUE),
('Nối tóc & tóc giả', 'hair-extensions-wigs', 3, 4, TRUE),
('Phụ kiện quần áo', 'clothes-accessories', 3, 5, TRUE),
('Đồng hồ & Phụ kiện', 'fashion-watches-accessories', 3, 6, TRUE),
('Vải may váy', 'dressmaking-fabrics', 3, 7, TRUE),
('Phụ kiện đám cưới', 'wedding-accessories', 3, 8, TRUE);

-- Seed Subcategories (menswear-underwear children)
INSERT INTO categories (name, slug, parent_id, sort_order, is_active) VALUES
('Áo nam', 'men-s-tops', 4, 1, TRUE),
('Quần nam', 'men-s-bottoms', 4, 2, TRUE),
('Bộ vét và quần yếm nam', 'men-s-suits-sets', 4, 3, TRUE),
('Đồ lót nam', 'men-s-underwear-socks', 4, 4, TRUE),
('Đồ ngủ & đồ mặc nhà cho nam', 'men-s-sleepwear-loungewear', 4, 5, TRUE),
('Trang phục đặc biệt dành cho nam', 'men-s-special-occasion-clothing', 4, 6, TRUE);

-- Seed Subcategories (home-supplies children)
INSERT INTO categories (name, slug, parent_id, sort_order, is_active) VALUES
('Đồ gia dụng', 'home-care-supplies', 5, 1, TRUE),
('Đồ dùng phòng tắm', 'bathroom-supplies', 5, 2, TRUE),
('Đồ đựng trong nhà', 'home-organizers', 5, 3, TRUE),
('Trang trí nội thất', 'home-decor', 5, 4, TRUE),
('Đồ dùng cho lễ hội & bữa tiệc', 'festive-party-supplies', 5, 5, TRUE),
('Dụng cụ & phụ kiện giặt là', 'laundry-tools-accessories', 5, 6, TRUE),
('Đồ gia dụng khác', 'miscellaneous-home', 5, 7, TRUE);

-- Seed Subcategories (beauty-personal-care children)
INSERT INTO categories (name, slug, parent_id, sort_order, is_active) VALUES
('Chăm sóc Tay & Chân', 'hand-foot-care', 6, 1, TRUE),
('Chăm sóc mắt & tai', 'eye-ear-care', 6, 2, TRUE),
('Thiết bị chăm sóc cá nhân', 'personal-care-appliances', 6, 3, TRUE),
('Trang điểm', 'makeup', 6, 4, TRUE),
('Chăm sóc da', 'skincare', 6, 5, TRUE),
('Chăm sóc & Tạo kiểu tóc', 'haircare-styling', 6, 6, TRUE),
('Chăm sóc mũi & răng miệng', 'nasal-oral-care', 6, 7, TRUE),
('Đồ tắm & Chăm sóc cơ thể', 'bath-body-care', 6, 8, TRUE),
('Nước hoa', 'fragrance', 6, 9, TRUE),
('Sản phẩm chăm sóc dành cho nam giới', 'men-s-care', 6, 10, TRUE),
('Sản phẩm chăm sóc dành cho phụ nữ', 'feminine-care', 6, 11, TRUE);

-- Seed Subcategories (shoes children)
INSERT INTO categories (name, slug, parent_id, sort_order, is_active) VALUES
('Phụ kiện giày', 'shoe-accessories', 7, 1, TRUE),
('Giày nữ', 'women-s-shoes', 7, 2, TRUE),
('Giày nam', 'men-s-shoes', 7, 3, TRUE);

-- Seed Subcategories (sports-outdoor children)
INSERT INTO categories (name, slug, parent_id, sort_order, is_active) VALUES
('Phụ kiện thể thao & ngoài trời', 'sports-outdoor-accessories', 8, 1, TRUE),
('Đồ thể thao & ngoài trời', 'sport-outdoor-clothing', 8, 2, TRUE),
('Thiết bị tập thể hình', 'fitness', 8, 3, TRUE),
('Đồ bơi, đồ lướt sóng & đồ lặn', 'swimwear-surfwear-wetsuits', 8, 4, TRUE),
('Giày thể thao', 'sports-footwear', 8, 5, TRUE),
('Thiết bị cắm trại & đi bộ đường dài', 'camping-hiking', 8, 6, TRUE),
('Thiết bị các môn thể thao bóng', 'ball-sports', 8, 7, TRUE),
('Thiết bị thể thao dưới nước', 'water-sports', 8, 8, TRUE);

-- Seed Subcategories (luggage-bags children)
INSERT INTO categories (name, slug, parent_id, sort_order, is_active) VALUES
('Túi xách nữ', 'women-s-bags', 9, 1, TRUE),
('Túi xách nam', 'men-s-bags', 9, 2, TRUE),
('Túi đa năng', 'functional-bags', 9, 3, TRUE),
('Hành lý & Túi du lịch', 'luggage-travel-bags', 9, 4, TRUE),
('Phụ kiện túi', 'bag-accessories', 9, 5, TRUE);

-- Seed Subcategories (toys-hobbies children)
INSERT INTO categories (name, slug, parent_id, sort_order, is_active) VALUES
('Đồ chơi cổ điển & mới lạ', 'classic-novelty-toys', 10, 1, TRUE),
('Búp bê & Gấu bông', 'dolls-stuffed-toys', 10, 2, TRUE),
('Trò chơi & Ghép hình', 'games-puzzles', 10, 3, TRUE),
('Đồ chơi thể thao & ngoài trời', 'sports-outdoor-play', 10, 4, TRUE),
('Đồ chơi giáo dục', 'educational-toys', 10, 5, TRUE),
('DIY', 'diy', 10, 6, TRUE),
('Đồ chơi điện & điều khiển từ xa', 'electric-remote-control-toys', 10, 7, TRUE),
('Nhạc cụ & Phụ kiện', 'musical-instruments-accessories', 10, 8, TRUE);
