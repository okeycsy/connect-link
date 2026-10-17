package com.connectlink.opinion.dto;

/**
 * 지금 사용자가 이 요청에 할 수 있는 일. 화면은 이 값으로 버튼을 보이고, 서버는 같은 규칙으로 다시 막는다.
 *
 * @param manage 결제원 쪽 화면(회신 현황·취합·이력)을 쓰는지
 * @param edit 내용·대상·양식·첨부를 고칠 수 있는지(작성중·회신중)
 * @param open 회신중으로 열 수 있는지(작성중)
 * @param close 마감할 수 있는지(회신중)
 * @param cancel 회신중 요청을 취소할 수 있는지
 * @param delete 작성중 요청을 지울 수 있는지
 * @param reply 기관 담당자로서 회신함을 쓰는지(대상 기관의 같은 업무 담당자)
 */
public record OpinionRequestPermissions(
    boolean manage,
    boolean edit,
    boolean open,
    boolean close,
    boolean cancel,
    boolean delete,
    boolean reply) {}
