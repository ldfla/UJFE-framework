package app.services;

import app.model.Bank;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class BrazilApiBankClientTest {
    @Test
    void parseBanksReadsStringAndNumericCodes() {
        String json = "["
            + "{\"ispb\":\"00000000\",\"code\":\"001\",\"name\":\"BCO DO BRASIL S.A.\","
            + "\"fullName\":\"Banco do Brasil S.A.\",\"ignored\":true},"
            + "{\"ispb\":\"00360305\",\"code\":104,\"name\":\"CAIXA ECONOMICA FEDERAL\","
            + "\"full_name\":\"Caixa Economica Federal\"}"
            + "]";
        List<Bank> banks = BrazilApiBankClient.parseBanks(json, 10);

        assertEquals(2, banks.size());
        assertEquals("001", banks.get(0).code());
        assertEquals("Banco do Brasil S.A.", banks.get(0).fullName());
        assertEquals("104", banks.get(1).code());
        assertEquals("Caixa Economica Federal", banks.get(1).fullName());
    }

    @Test
    void parseBanksAppliesDefaultsForMissingFields() {
        List<Bank> banks = BrazilApiBankClient.parseBanks("[{\"code\":260}]", 10);

        assertEquals(1, banks.size());
        assertEquals("", banks.get(0).ispb());
        assertEquals("260", banks.get(0).code());
        assertEquals("Banco sem nome", banks.get(0).name());
        assertEquals("Banco sem nome", banks.get(0).fullName());
    }

    @Test
    void parseBanksHandlesEmptyBlankAndInvalidPayloadsWithoutNetwork() {
        assertTrue(BrazilApiBankClient.parseBanks("[]", 10).isEmpty());
        assertTrue(BrazilApiBankClient.parseBanks("", 10).isEmpty());
        assertTrue(BrazilApiBankClient.parseBanks("not json", 10).isEmpty());
        assertThrows(NullPointerException.class, () -> BrazilApiBankClient.parseBanks(null, 10));
    }

    @Test
    void parseBanksHonorsMaximumItemsAndPreservesDuplicateCodes() {
        String json = "["
            + "{\"ispb\":\"1\",\"code\":\"001\",\"name\":\"First\"},"
            + "{\"ispb\":\"2\",\"code\":\"001\",\"name\":\"Duplicate\"},"
            + "{\"ispb\":\"3\",\"code\":\"003\",\"name\":\"Third\"}"
            + "]";
        List<Bank> banks = BrazilApiBankClient.parseBanks(json, 2);

        assertEquals(2, banks.size());
        assertEquals(List.of("001", "001"), banks.stream().map(Bank::code).collect(Collectors.toList()));
    }
}
