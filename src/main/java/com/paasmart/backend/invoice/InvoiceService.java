package com.paasmart.backend.invoice;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderItem;
import com.paasmart.backend.order.OrderItemRepository;
import com.paasmart.backend.order.OrderRepository;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoiceService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;

    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    public byte[] generateInvoice(Long requesterId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Shop shop = shopRepository.findById(order.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        boolean isCustomer = order.getCustomerId().equals(requesterId);
        boolean isSeller = shop.getSellerId().equals(requesterId);
        if (!isCustomer && !isSeller) {
            throw new UnauthorizedException("You are not authorized to view this invoice");
        }

        User customer = userRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                float pageWidth = page.getMediaBox().getWidth() - 2 * margin;

                // ---- Header ----
                content.beginText();
                content.setFont(FONT_BOLD, 20);
                content.newLineAtOffset(margin, y);
                content.showText("PaaSmart");
                content.endText();

                content.beginText();
                content.setFont(FONT_REGULAR, 10);
                content.newLineAtOffset(margin, y - 18);
                content.showText("Tax Invoice");
                content.endText();
                y -= 45;

                // ---- Order & Shop info ----
                content.setFont(FONT_BOLD, 11);
                y = writeLine(content, margin, y, "Order #" + order.getId(), FONT_BOLD, 11);
                y = writeLine(content, margin, y,
                        "Date: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")),
                        FONT_REGULAR, 10);
                y = writeLine(content, margin, y, "Sold By: " + shop.getShopName(), FONT_REGULAR, 10);
                y -= 10;

                y = writeLine(content, margin, y, "Billed To:", FONT_BOLD, 11);
                y = writeLine(content, margin, y, customer.getName(), FONT_REGULAR, 10);
                y = writeLine(content, margin, y, customer.getPhone(), FONT_REGULAR, 10);
                y = writeLine(content, margin, y, "Delivery Address: " + order.getDeliveryAddress(), FONT_REGULAR, 10);
                y -= 15;

                // ---- Table header ----
                content.setLineWidth(0.5f);
                content.moveTo(margin, y);
                content.lineTo(margin + pageWidth, y);
                content.stroke();
                y -= 15;

                content.beginText();
                content.setFont(FONT_BOLD, 10);
                content.newLineAtOffset(margin, y);
                content.showText("Item");
                content.newLineAtOffset(260, 0);
                content.showText("Qty");
                content.newLineAtOffset(50, 0);
                content.showText("Unit Price");
                content.newLineAtOffset(90, 0);
                content.showText("Subtotal");
                content.endText();
                y -= 8;

                content.moveTo(margin, y);
                content.lineTo(margin + pageWidth, y);
                content.stroke();
                y -= 18;

                // ---- Items ----
                for (OrderItem item : items) {
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    String name = product != null ? product.getName() : ("Product #" + item.getProductId());
                    if (name.length() > 35) name = name.substring(0, 32) + "...";

                    content.beginText();
                    content.setFont(FONT_REGULAR, 10);
                    content.newLineAtOffset(margin, y);
                    content.showText(name);
                    content.newLineAtOffset(260, 0);
                    content.showText(String.valueOf(item.getQuantity()));
                    content.newLineAtOffset(50, 0);
                    content.showText("Rs. " + item.getUnitPrice());
                    content.newLineAtOffset(90, 0);
                    content.showText("Rs. " + item.getSubtotal());
                    content.endText();
                    y -= 20;
                }

                y -= 5;
                content.moveTo(margin, y);
                content.lineTo(margin + pageWidth, y);
                content.stroke();
                y -= 20;

                // ---- Totals ----
                BigDecimal itemsTotal = items.stream()
                        .map(OrderItem::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                y = writeRightAlignedLine(content, margin, pageWidth, y, "Items Total:", "Rs. " + itemsTotal);
                y = writeRightAlignedLine(content, margin, pageWidth, y, "Delivery Fee:", "Rs. " + order.getDeliveryFee());
                y -= 5;
                content.moveTo(margin + pageWidth - 200, y);
                content.lineTo(margin + pageWidth, y);
                content.stroke();
                y -= 18;
                y = writeRightAlignedLine(content, margin, pageWidth, y, "Grand Total:", "Rs. " + order.getTotalAmount());

                y -= 15;
                y = writeLine(content, margin, y, "Payment Mode: " + order.getPaymentMode(), FONT_REGULAR, 10);
                y = writeLine(content, margin, y, "Order Status: " + order.getStatus(), FONT_REGULAR, 10);

                y -= 30;
                content.beginText();
                content.setFont(FONT_REGULAR, 8);
                content.newLineAtOffset(margin, y);
                content.showText("This is a computer-generated invoice and does not require a signature.");
                content.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate invoice: " + e.getMessage());
        }
    }

    private float writeLine(PDPageContentStream content, float x, float y, String text, PDFont font, float size) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - (size + 6);
    }

    private float writeRightAlignedLine(PDPageContentStream content, float margin, float pageWidth, float y, String label, String value) throws IOException {
        content.beginText();
        content.setFont(FONT_REGULAR, 10);
        content.newLineAtOffset(margin + pageWidth - 200, y);
        content.showText(label);
        content.endText();

        content.beginText();
        content.setFont(FONT_BOLD, 10);
        content.newLineAtOffset(margin + pageWidth - 90, y);
        content.showText(value);
        content.endText();

        return y - 18;
    }
}