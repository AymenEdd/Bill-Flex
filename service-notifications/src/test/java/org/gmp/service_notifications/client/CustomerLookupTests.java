package org.gmp.service_notifications.client;

import feign.FeignException;
import org.gmp.service_notifications.entity.NotificationFailure;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerLookupTests {

    @Test
    void retriesTransientErrorsAndOpensCircuitAfterRepeatedFailures() {
        CustomerClient client = mock(CustomerClient.class);
        FeignException temporaryFailure = mock(FeignException.class);
        when(temporaryFailure.status()).thenReturn(503);
        when(client.findById(1L)).thenThrow(temporaryFailure);
        CustomerLookup lookup = new CustomerLookup(client);

        for (int failure = 0; failure < 4; failure++) {
            assertThatThrownBy(() -> lookup.fetchEmail(1L)).isInstanceOf(CustomerLookup.Unavailable.class);
        }
        assertThatThrownBy(() -> lookup.fetchEmail(1L)).isInstanceOf(CustomerLookup.Unavailable.class);
        verify(client, times(12)).findById(1L);
    }

    @Test
    void rejects404WithoutRetryOrOpeningCircuit() {
        CustomerClient client = mock(CustomerClient.class);
        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);
        when(client.findById(1L)).thenThrow(notFound);
        when(client.findById(2L)).thenReturn(new CustomerClient.CustomerResponse(2L, "test@bill-flex.test"));
        CustomerLookup lookup = new CustomerLookup(client);

        for (int missing = 0; missing < 5; missing++) {
            assertThatThrownBy(() -> lookup.fetchEmail(1L)).isInstanceOfSatisfying(
                    CustomerLookup.Rejected.class,
                    error -> assertThat(error.getFailure()).isEqualTo(NotificationFailure.CLIENT_INTROUVABLE));
        }
        verify(client, times(5)).findById(1L);
        assertThat(lookup.fetchEmail(2L)).isEqualTo("test@bill-flex.test");
    }
}
