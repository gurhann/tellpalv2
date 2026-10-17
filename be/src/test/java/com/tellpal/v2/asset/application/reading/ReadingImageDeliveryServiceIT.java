package com.tellpal.v2.asset.application.reading;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi.*;

class ReadingImageDeliveryServiceIT {
    @Test void explicitMemoryArithmeticRejectsOverflowAndInsufficientPeak() {
        assertThatThrownBy(()->new Limits(100,Long.MAX_VALUE,Long.MAX_VALUE,1000,"synthetic",10,10000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new Limits(100,100,1999,1000,"synthetic",10,10000)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(()->new Limits(100,100,2000,1000,"synthetic",10,10000)).doesNotThrowAnyException();
    }
    @Test void sdkDeadlineRangeRejectsAboveIntBeforeAnyIo() {
        assertThatCode(()->new Limits(100,100,2000,Integer.MAX_VALUE,"synthetic",1,100)).doesNotThrowAnyException();
        assertThatThrownBy(()->new Limits(100,100,2000,(long)Integer.MAX_VALUE+1,"synthetic",1,100)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void actualHeaderAboveDeliveryPixelBudgetRefusesBeforeFullDecode() throws Exception {
        byte[] image=com.tellpal.v2.asset.reading.ReadingFixtures.image(80,60);
        assertThatThrownBy(()->ReadingImageDeliveryService.requireActualRaster(image,100))
                .hasMessageContaining("ACTUAL_RASTER_LIMIT");
    }
    @Test void concurrentAdmissionKeepsCapacityUntilPhysicalOwnerCloses() {
        try(var owner=ReadingPreparationCapacity.acquire()) {
            assertThatThrownBy(ReadingPreparationCapacity::acquire).isInstanceOf(ReadingVerificationInconclusiveException.class);
        }
        try(var next=ReadingPreparationCapacity.acquire()) { assertThat(next).isNotNull(); }
    }
}
