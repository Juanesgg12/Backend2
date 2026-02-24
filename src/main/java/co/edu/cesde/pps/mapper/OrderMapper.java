package co.edu.cesde.pps.mapper;

import co.edu.cesde.pps.dto.OrderDTO;
import co.edu.cesde.pps.dto.OrderItemDTO;
import co.edu.cesde.pps.model.Order;
import co.edu.cesde.pps.model.OrderItem;
import co.edu.cesde.pps.util.MoneyUtils;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversión entre Order/OrderItem (Entities) y OrderDTO/OrderItemDTO.
 *
 * Responsabilidades:
 * - Convertir Entity a DTO (toDTO)
 * - Convertir DTO a Entity (toEntity)
 * - Manejar null safety
 * - Convertir items anidados
 * - Formatear valores monetarios
 */
public class OrderMapper {


    /**
     * Convierte Order Entity a OrderDTO.
     *
     * @param order Entity a convertir
     * @return OrderDTO o null si order es null
     */
    public OrderDTO toDTO(Order order) {
        if (order == null) {
            return null;
        }

        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setOrderNumber(order.getOrderNumber());
        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getUserId());
        }

        // TODO: En etapa 06 con JPA, cargar user para obtener email y fullName
        // Por ahora solo tenemos userId

        // TODO: En etapa 06 con JPA, cargar orderStatus para obtener nombre
        // Por ahora solo tenemos orderStatusId

        // TODO: En etapa 06 con JPA, cargar addresses completas
        // Por ahora solo tenemos addressIds

        dto.setCreatedAt(order.getCreatedAt());

        // Convertir items
        if (order.getItems() != null) {
            List<OrderItemDTO> itemDTOs = order.getItems().stream()
                    .map(this::toOrderItemDTO)
                    .collect(Collectors.toList());
            dto.setItems(itemDTOs);
            dto.setItemsCount(itemDTOs.size());
        } else {
            dto.setItemsCount(0);
        }

        // Valores monetarios
        dto.setSubtotal(order.getSubtotal());
        dto.setTax(order.getTax());
        dto.setShippingCost(order.getShippingCost());
        dto.setTotal(order.getTotal());

        // Formatear valores monetarios
        if (dto.getSubtotal() != null) {
            dto.setSubtotalFormatted(MoneyUtils.formatUSD(dto.getSubtotal()));
        }
        if (dto.getTax() != null) {
            dto.setTaxFormatted(MoneyUtils.formatUSD(dto.getTax()));
        }
        if (dto.getShippingCost() != null) {
            dto.setShippingCostFormatted(MoneyUtils.formatUSD(dto.getShippingCost()));
        }
        if (dto.getTotal() != null) {
            dto.setTotalFormatted(MoneyUtils.formatUSD(dto.getTotal()));
        }

        return dto;
    }

    /**
     * Convierte OrderItem Entity a OrderItemDTO.
     *
     * @param item Entity a convertir
     * @return OrderItemDTO o null si item es null
     */
    public OrderItemDTO toOrderItemDTO(OrderItem item) {
        if (item == null) {
            return null;
        }

        OrderItemDTO dto = new OrderItemDTO();
        dto.setOrderItemId(item.getOrderItemId());

        if (item.getOrder() != null) {
            dto.setOrderId(item.getOrder().getOrderId());
        }

        // Extraer datos de Product (relación)
        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getProductId());
            dto.setProductName(item.getProduct().getName());
            dto.setProductSku(item.getProduct().getSku());
        }

        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice()); // Precio histórico
        dto.setLineTotal(item.getLineTotal());

        // Formatear valores
        if (dto.getUnitPrice() != null) {
            dto.setUnitPriceFormatted(MoneyUtils.formatUSD(dto.getUnitPrice()));
        }
        if (dto.getLineTotal() != null) {
            dto.setLineTotalFormatted(MoneyUtils.formatUSD(dto.getLineTotal()));
        }

        return dto;
    }

    /**
     * Convierte OrderDTO a Order Entity.
     *
     * NOTA: No convierte items completamente, eso se maneja en el servicio.
     *
     * @param dto DTO a convertir
     * @return Order Entity o null si dto es null
     */
    public Order toEntity(OrderDTO dto) {
        if (dto == null) {
            return null;
        }

        Order order = new Order();
        order.setOrderId(dto.getOrderId());
        order.setOrderNumber(dto.getOrderNumber());
        // user, orderStatus, shippingAddress, billingAddress se asignan en servicio
        order.setSubtotal(dto.getSubtotal());
        order.setTax(dto.getTax());
        order.setShippingCost(dto.getShippingCost());
        order.setTotal(dto.getTotal());
        order.setCreatedAt(dto.getCreatedAt());

        return order;
    }

    /**
     * Convierte OrderItemDTO a OrderItem Entity.
     *
     * NOTA: No convierte Order ni Product, eso se maneja en el servicio.
     *
     * @param dto DTO a convertir
     * @return OrderItem Entity o null si dto es null
     */
    public OrderItem toOrderItemEntity(OrderItemDTO dto) {
        if (dto == null) {
            return null;
        }

        OrderItem item = new OrderItem();
        item.setOrderItemId(dto.getOrderItemId());
        // Order y Product se deben asignar en el servicio
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(dto.getUnitPrice());
        item.setLineTotal(dto.getLineTotal());

        return item;
    }

    /**
     * Convierte lista de Order Entities a lista de OrderDTOs.
     *
     * @param orders Lista de entities
     * @return Lista de DTOs o lista vacía si orders es null
     */
    public List<OrderDTO> toDTOList(List<Order> orders) {
        if (orders == null) {
            return List.of();
        }

        return orders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
