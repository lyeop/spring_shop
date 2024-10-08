package com.example.Spring_shop.repository;

import com.example.Spring_shop.constant.PaymentStatus;
import com.example.Spring_shop.dto.CartOrderDto;
import com.example.Spring_shop.dto.PaymentCallbackRequest;

import com.example.Spring_shop.dto.RequestPayDto;
import com.example.Spring_shop.entity.Member;
import com.example.Spring_shop.entity.Order;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;


import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final IamportClient iamportClient;
    @Override
    public RequestPayDto findRequestDto(Long id) {
        System.out.println("쿼리문 확인~!~!~!~!~!~!~!~!~!~!~!~!~!");
        Order order = orderRepository.findOrderAndPaymentAndMember(id)
                .orElseThrow(() -> new IllegalArgumentException("주문이 없습니다."));
        System.out.println("빌더66666666666666666666666666666666666666");
        System.out.println("아이템아이디"+ id);

        Member.Address address1 = new Member.Address(order.getMember().getAddress().getZipcode(),
                order.getMember().getAddress().getStreetAdr(),order.getMember().getAddress().getDetailAdr());
        System.out.println(" 오더 아이템 이름" + order.getItemNmList());


        return RequestPayDto.builder()
                .buyerName(order.getMember().getName())
                .buyerEmail(order.getMember().getEmail())
                .buyerAddress(address1)
                .orderPrice(order.getTotalPrice())
                .itemNm(order.getItemNmList())
                .orderUid(order.getOrderUid())
                .build();
    }

    @Override
    public IamportResponse<Payment> paymentByCallback(PaymentCallbackRequest request) {
        try{
            //결제 단건 조회(아임포트)
            IamportResponse<Payment> iamportResponse = iamportClient.paymentByImpUid(request.getPaymentUid());
            //주문내역 조회
            Order order = orderRepository.findOrderAndPayment(request.getOrderUid())
                    .orElseThrow(()-> new IllegalArgumentException("주문 내역이 없습니다."));

            //결제 완료가 아니면
            if(!iamportResponse.getResponse().getStatus().equals("paid")){
                //주문,결제 삭제
                orderRepository.delete(order);
                paymentRepository.delete(order.getPayment());

                throw new RuntimeException("결제 미완료");
            }
            //DB에 저장된 결제 금액
            int orderPrice = order.getOrderPrice();
            // 실 결제 금액
            int iamportPrice = iamportResponse.getResponse().getAmount().intValue();
            //결제 금액 검증
            if(iamportPrice != orderPrice){
                //주문,결제 삭제
                orderRepository.delete(order);
                paymentRepository.delete(order.getPayment());

                //결제금액 위변조로 의심되는 결제금액을 취소(아임포트)
                iamportClient.cancelPaymentByImpUid(new CancelData(iamportResponse.getResponse().getImpUid(),true,new BigDecimal(iamportPrice)));

                throw new RuntimeException("결제금액 위변조 의심");
            }
            //결제 상태 변경
            order.getPayment().changePaymentBySuccess(PaymentStatus.OK,iamportResponse.getResponse().getImpUid());

            return iamportResponse;

        }catch (IamportResponseException e){
            throw new RuntimeException(e);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}