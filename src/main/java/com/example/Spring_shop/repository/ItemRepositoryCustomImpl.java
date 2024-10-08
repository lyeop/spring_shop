package com.example.Spring_shop.repository;

import com.example.Spring_shop.constant.ItemSellStatus;
import com.example.Spring_shop.constant.ItemValue;
import com.example.Spring_shop.dto.ItemSearchDto;
import com.example.Spring_shop.dto.MainItemDto;
import com.example.Spring_shop.dto.QMainItemDto;
import com.example.Spring_shop.entity.Item;
import com.example.Spring_shop.entity.QItem;
import com.example.Spring_shop.entity.QItemImg;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.thymeleaf.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

public class ItemRepositoryCustomImpl implements ItemRepositoryCustom{
    private JPAQueryFactory queryFactory; // 동적 쿼리 사용하기 위해 JPAQueryFactory 변수 선언
    // 생성자
    public ItemRepositoryCustomImpl(EntityManager em){
        this.queryFactory = new JPAQueryFactory(em); // JPAQueryFactory 실질적인 객체가 만들어 집니다.
    }
    private BooleanExpression searchSellStatusEq(ItemSellStatus searchSellStatus){
        return searchSellStatus == null ?
                null : QItem.item.itemSellStatus.eq(searchSellStatus);
        // ItemSellStatus null 이면 null 리턴, null 아니면 SELL, SOLD 둘중 하나 리턴
    }
    private BooleanExpression regDtsAfter(String searchDateType){ // all, 1d, 1w, 1m, 6m
        LocalDateTime dateTime = LocalDateTime.now(); // 현재 시간을 추출해서 변수에 대입

        if (StringUtils.equals("all", searchDateType) || searchDateType == null){
            return null;
        }
        else if (StringUtils.equals("1d", searchDateType)){
            dateTime = dateTime.minusDays(1);
        }
        else if (StringUtils.equals("1w", searchDateType)){
            dateTime = dateTime.minusWeeks(1);
        }
        else if (StringUtils.equals("1m", searchDateType)){
            dateTime = dateTime.minusMonths(1);
        }
        else if (StringUtils.equals("6m", searchDateType)){
            dateTime = dateTime.minusMonths(1);
        }
        return QItem.item.regTime.after(dateTime);
        // dateTime 을 시간에 맞게 세팅 후 시간에 맞는 등록된 상품이 조회하도록 조건값 반환
    }
    private BooleanExpression searchByLike(String searchBy, String searchQuery){
        if (StringUtils.equals("itemNm", searchBy)){ // 상품명
            return QItem.item.itemNm.like("%" + searchQuery + "%");
        }
        else if (StringUtils.equals("createdBy", searchBy)){ // 작성자
            return QItem.item.createdBy.like("%" + searchQuery + "%");
        }
        return null;
    }
    @Override
    public Page<Item> getAdminItemPage(ItemSearchDto itemSearchDto, Pageable pageable){
        QueryResults<Item> results = queryFactory.selectFrom(QItem.item)
                .where(regDtsAfter(itemSearchDto.getSearchDateType()),
                        searchSellStatusEq(itemSearchDto.getSearchSellStatus()),
                        searchByLike(itemSearchDto.getSearchBy(), itemSearchDto.getSearchQuery()))
                .orderBy(QItem.item.id.desc())
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetchResults();
        List<Item> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }
    private BooleanExpression itemValueEq(ItemValue itemValue) {
        return itemValue == null ? null : QItem.item.itemValue.eq(itemValue);
    }
    private BooleanExpression itemNmLike(String searchQuery){
        return StringUtils.isEmpty(searchQuery) ? null : QItem.item.itemNm.like("%" + searchQuery + "%");
    }
    @Override
    public Page<MainItemDto> getMainItemPage(ItemSearchDto itemSearchDto, Pageable pageable){
        QItem item = QItem.item;
        QItemImg itemImg = QItemImg.itemImg;
        // select i.id, id.itemNm, i.itemDetail, im.itemImg, i.price from item i, itemImg im join i.id = im.itemId
        // where im.repImgYn = "Y" and i.itemNm like %searchQuery% order by i.id desc
        // QMainItemDto @QueryProjection 을 허용하면 DTO 로 바로 조회 가능
        QueryResults<MainItemDto> results = queryFactory.select(new QMainItemDto(item.id, item.itemNm,
                        item.itemDetail, itemImg.imgUrl, item.price, item.endDate , item.BidPrice))
                // join 내부조인 .repImgYn.eq("Y") 대표이미지만 가져온다.
                .from(itemImg).join(itemImg.item).where(itemImg.repImgYn.eq("Y"))
                .where(itemNmLike(itemSearchDto.getSearchQuery()))
                .orderBy(item.id.desc()).offset(pageable.getOffset()).limit(pageable.getPageSize()).fetchResults();
        List<MainItemDto> content = results.getResults();
        long total = results.getTotal();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MainItemDto> getItemPage(ItemSearchDto itemSearchDto, Pageable pageable) {
        QItem item = QItem.item;
        QItemImg itemImg = QItemImg.itemImg;

        QueryResults<MainItemDto> results = queryFactory.select(new QMainItemDto(
                        item.id,
                        item.itemNm,
                        item.itemDetail,
                        itemImg.imgUrl,
                        item.price,
                        item.endDate,
                        item.BidPrice
                ))
                .from(itemImg)
                .join(itemImg.item)
                .where(itemImg.repImgYn.eq("Y"))
                .where(itemValueEq(itemSearchDto.getItemValue()))
                .orderBy(item.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MainItemDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }
}
