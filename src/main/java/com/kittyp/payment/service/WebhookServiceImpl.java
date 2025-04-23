/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.kittyp.common.util.Mapper;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.payment.entity.WebhookEvent;
import com.kittyp.payment.enums.WebhookSource;
import com.kittyp.payment.model.RazorpayResponseModel;
import com.kittyp.payment.model.RazorpayResponseModel.PaymentEntity;
import com.kittyp.payment.repository.WebhookEventRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

	private final Mapper mapper;
	private final WebhookEventRepository webhookEventRepository;
	private final OrderDao orderDao;
	/**
	 * @author rrohan419@gmail.com
	 */
	@Async
	@Override
	public void razorpayWebbhook(RazorpayResponseModel razorpayResponseModel) {
		
		WebhookEvent webhookEvent = new WebhookEvent();
		
		PaymentEntity paymentEntity = razorpayResponseModel.getPayload().getPayment().getEntity();
		
		webhookEvent.setSource(WebhookSource.RAZORPAY);
		webhookEvent.setEventType(razorpayResponseModel.getEvent());
		webhookEvent.setPayload(mapper.convertObjectToJson(razorpayResponseModel));
		webhookEvent.setPaymentId(paymentEntity.getId());
		webhookEvent.setStatus(paymentEntity.getStatus());
		webhookEvent.setErrorMessage(paymentEntity.getError_reason());
		webhookEvent.setRetryCount(0);
		webhookEvent.setOrderId(paymentEntity.getOrder_id());
		
		webhookEventRepository.save(webhookEvent);
		
		Order order = orderDao.orderByAggregatorOrderNumber(paymentEntity.getOrder_id());
		OrderStatus status = OrderStatus.fromRazorpayStatus(paymentEntity.getStatus());
		order.setStatus(status);
		
		orderDao.saveOrder(order);
		
		System.out.println(razorpayResponseModel);
		
		System.out.println("wqwwew");

	}

}
