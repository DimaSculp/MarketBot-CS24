package ru.outfix.market.ad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdDraftTest {

    @Test
    void titleIsLimitedTo45Chars() {
        assertEquals("Ошибка: название превышает 45 символов.", AdDraft.validateTitle("x".repeat(46)).orElseThrow());
        assertTrue(AdDraft.validateTitle("x".repeat(45)).isEmpty());
        assertEquals("Ошибка: название не может быть пустым.", AdDraft.validateTitle("   ").orElseThrow());
    }

    @Test
    void descriptionIsLimitedTo700Chars() {
        assertEquals("Ошибка: описание превышает 700 символов.", AdDraft.validateDescription("a".repeat(701)).orElseThrow());
        assertTrue(AdDraft.validateDescription("a".repeat(700)).isEmpty());
    }

    @Test
    void priceMustNotBeNegative() {
        assertEquals("Ошибка: цена не может быть отрицательной.", AdDraft.validatePrice(-1).orElseThrow());
        assertTrue(AdDraft.validatePrice(0).isEmpty());
    }
}
