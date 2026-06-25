# Addon modules — hướng dẫn tích hợp

5 file Java cho addon Meteor Client (kiểu meteor-rejects):

| File | Chức năng |
|---|---|
| `AutoLumberjack.java` | Tự trồng/chặt cây, hỗ trợ giant tree 2x2, có toggle `instant-fell` cho server treecapitator-style |
| `AutoJunkDiscard.java` | Tự vứt item thừa, chỉ giữ whitelist (mặc định: sapling) |
| `GuiShopUtils.java` | Helper dùng chung để thao tác GUI shop (click theo tên item) |
| `AutoBuyExp.java` | Tự mở shop, mua trái kinh nghiệm |
| `AutoRepair.java` | Tự sửa công cụ/giáp khi sắp hỏng (qua lệnh hoặc GUI shop) |

## 1. Copy & đăng ký

Copy cả 5 file vào package `modules` trong addon hiện có, sửa dòng `package` đầu mỗi file
cho khớp project. Trong `Addon` class, thêm vào `onInitialize()`:

```java
Modules.get().add(new AutoLumberjack());
Modules.get().add(new AutoJunkDiscard());
Modules.get().add(new AutoBuyExp());
Modules.get().add(new AutoRepair());
```

(`GuiShopUtils` không phải module, không cần đăng ký — chỉ là class helper dùng chung.)

## 2. Việc BẮT BUỘC m phải tự điền (t không có cách nào biết KingMC dùng gì)

**AutoBuyExp:**
- `open-command` — lệnh/cách mở shop trên KingMC.
- `item-name` — chuỗi tên hiển thị của "trái kinh nghiệm" trong GUI shop.
- `confirm-name` — tên nút xác nhận mua, nếu shop có bước phụ (để trống nếu không có).

**AutoRepair:**
- `method` — chọn `Command` nếu server có lệnh kiểu `/repair`, chọn `ShopGui` nếu phải
  mở GUI sửa đồ và click xác nhận.
- Điền lệnh/tên item tương ứng.

**Cách lấy đúng chuỗi tên item:** mở shop/GUI thủ công trong game, đưa item ra inventory
hoặc hover xem tên đầy đủ — copy phần chữ chính (không cần giữ mã màu §x).

## 3. instant-fell trong AutoLumberjack

Vì server m có rìu/plugin chặt rụng cả cây 1 nhát, bật `instant-fell = true` —
module sẽ chỉ break 1 block log gốc thay vì flood-fill từng log như vanilla,
nhanh hơn nhiều và đúng với cơ chế server m đang chơi.

## 4. Lưu ý chung

- Các tên hàm Meteor Client (`InvUtils`, `BlockUtils`, `ChatUtils`...) có thể lệch
  chút theo version cụ thể m đang build — đối chiếu lại với source `meteor-rejects`
  hiện có nếu compile lỗi, logic tổng thể không cần đổi.
- `Baritone.pathTo()` trong `AutoLumberjack` vẫn để TODO như lần trước.
- Build qua GitHub Actions như cách m đang làm, không cần máy tính.
- AutoBuyExp/AutoRepair dùng state machine đơn giản (chờ N tick giữa các bước) —
  nếu server lag hoặc GUI mở chậm, tăng `step-delay`/`cooldown` lên cho chắc.
