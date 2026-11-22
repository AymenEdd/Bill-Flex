package org.gmp.servicefacturation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.gmp.servicefacturation.controller.InvoiceController;
import org.gmp.servicefacturation.dto.InvoiceLineRequest;
import org.gmp.servicefacturation.dto.InvoiceRequest;
import org.gmp.servicefacturation.dto.InvoiceResponse;
import org.gmp.servicefacturation.entity.StatutFacture;
import org.gmp.servicefacturation.exception.GlobalExceptionHandler;
import org.gmp.servicefacturation.exception.IllegalStateTransitionException;
import org.gmp.servicefacturation.exception.ResourceNotFoundException;
import org.gmp.servicefacturation.scheduler.InvoiceScheduler;
import org.gmp.servicefacturation.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InvoiceScheduler invoiceScheduler;

    @InjectMocks
    private InvoiceController invoiceController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(invoiceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /invoices/{id} renvoie 200 et la facture")
    void testGetInvoiceById_Success() throws Exception {
        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .subscriptionId(10L)
                .clientId(20L)
                .periode("2026-11")
                .montantHT(new BigDecimal("100.00"))
                .montantTTC(new BigDecimal("120.00"))
                .statut(StatutFacture.BROUILLON)
                .lines(List.of())
                .build();

        when(invoiceService.getInvoiceById(1L)).thenReturn(response);

        mockMvc.perform(get("/invoices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.montantHT").value(100.00))
                .andExpect(jsonPath("$.statut").value("BROUILLON"));
    }

    @Test
    @DisplayName("GET /invoices/{id} renvoie 404 si introuvable")
    void testGetInvoiceById_NotFound() throws Exception {
        when(invoiceService.getInvoiceById(999L)).thenThrow(new ResourceNotFoundException("Facture introuvable"));

        mockMvc.perform(get("/invoices/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Facture introuvable"));
    }

    @Test
    @DisplayName("POST /invoices/{id}/emit avec transition interdite renvoie 409 Conflict")
    void testEmitInvoice_Conflict() throws Exception {
        when(invoiceService.emitInvoice(1L))
                .thenThrow(new IllegalStateTransitionException("Transition interdite : Seule une facture en statut BROUILLON peut être émise"));

        mockMvc.perform(post("/invoices/1/emit"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Transition interdite : Seule une facture en statut BROUILLON peut être émise"));
    }

    @Test
    @DisplayName("POST /invoices/{id}/lines ajoute une ligne et renvoie 201 Created")
    void testAddLine_Success() throws Exception {
        InvoiceLineRequest lineRequest = InvoiceLineRequest.builder()
                .libelle("Consulting")
                .quantite(2)
                .prixUnitaire(new BigDecimal("150.00"))
                .build();

        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .montantHT(new BigDecimal("300.00"))
                .montantTTC(new BigDecimal("360.00"))
                .statut(StatutFacture.BROUILLON)
                .build();

        when(invoiceService.addLineToInvoice(eq(1L), any(InvoiceLineRequest.class))).thenReturn(response);

        mockMvc.perform(post("/invoices/1/lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lineRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montantHT").value(300.00));
    }
}
