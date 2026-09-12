package com.kittyp.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;

import lombok.RequiredArgsConstructor;

/**
 * Applies verified WhatsApp credentials + refreshes cached connection/template status.
 */
@Service
@RequiredArgsConstructor
public class WhatsAppConnectionService {

	private final ClinicRepository clinicRepository;
	private final DoctorProfileDao doctorProfileDao;
	private final WhatsAppTemplateSetupService templateSetupService;

	@Transactional
	public Map<String, Object> connectClinic(
			Clinic clinic,
			String token,
			String phoneNumberId,
			String wabaId,
			boolean ensureTemplates) {
		clinic.setWhatsappToken(token);
		clinic.setWhatsappPhoneNumberId(phoneNumberId);
		clinic.setWhatsappBusinessAccountId(wabaId);
		clinic.setWhatsappConnectionStatus(WhatsAppConnectionStatuses.CONNECTED);
		clinic.setWhatsappConnectedAt(LocalDateTime.now());
		clinic.setWhatsappLastVerifiedAt(LocalDateTime.now());
		clinicRepository.save(clinic);

		Map<String, Object> templates = ensureTemplates
				? templateSetupService.ensureKittyPTemplates(token, wabaId)
				: templateSetupService.templateStatus(token, wabaId);
		String invoiceStatus = resolveInvoiceStatus(templates);
		clinic.setWhatsappInvoiceTemplateStatus(invoiceStatus);
		clinicRepository.save(clinic);

		return WhatsAppSettingsSupport.publicViewFull(
				clinic.getWhatsappPhoneNumberId(),
				clinic.getWhatsappBusinessAccountId(),
				clinic.getWhatsappToken(),
				clinic.getWhatsappConnectionStatus(),
				clinic.getWhatsappInvoiceTemplateStatus(),
				templates);
	}

	@Transactional
	public Map<String, Object> connectDoctor(
			DoctorProfile profile,
			String token,
			String phoneNumberId,
			String wabaId,
			boolean ensureTemplates) {
		profile.setWhatsappToken(token);
		profile.setWhatsappPhoneNumberId(phoneNumberId);
		profile.setWhatsappBusinessAccountId(wabaId);
		profile.setWhatsappConnectionStatus(WhatsAppConnectionStatuses.CONNECTED);
		profile.setWhatsappConnectedAt(LocalDateTime.now());
		profile.setWhatsappLastVerifiedAt(LocalDateTime.now());
		doctorProfileDao.save(profile);

		Map<String, Object> templates = ensureTemplates
				? templateSetupService.ensureKittyPTemplates(token, wabaId)
				: templateSetupService.templateStatus(token, wabaId);
		String invoiceStatus = resolveInvoiceStatus(templates);
		profile.setWhatsappInvoiceTemplateStatus(invoiceStatus);
		doctorProfileDao.save(profile);

		return WhatsAppSettingsSupport.publicViewFull(
				profile.getWhatsappPhoneNumberId(),
				profile.getWhatsappBusinessAccountId(),
				profile.getWhatsappToken(),
				profile.getWhatsappConnectionStatus(),
				profile.getWhatsappInvoiceTemplateStatus(),
				templates);
	}

	@Transactional
	public Map<String, Object> refreshClinicTemplates(Clinic clinic, boolean ensure) {
		Map<String, Object> templates = ensure
				? templateSetupService.ensureKittyPTemplates(clinic.getWhatsappToken(), clinic.getWhatsappBusinessAccountId())
				: templateSetupService.templateStatus(clinic.getWhatsappToken(), clinic.getWhatsappBusinessAccountId());
		clinic.setWhatsappInvoiceTemplateStatus(resolveInvoiceStatus(templates));
		clinic.setWhatsappLastVerifiedAt(LocalDateTime.now());
		clinicRepository.save(clinic);
		return WhatsAppSettingsSupport.publicViewFull(
				clinic.getWhatsappPhoneNumberId(),
				clinic.getWhatsappBusinessAccountId(),
				clinic.getWhatsappToken(),
				clinic.getWhatsappConnectionStatus(),
				clinic.getWhatsappInvoiceTemplateStatus(),
				templates);
	}

	@Transactional
	public Map<String, Object> refreshDoctorTemplates(DoctorProfile profile, boolean ensure) {
		Map<String, Object> templates = ensure
				? templateSetupService.ensureKittyPTemplates(profile.getWhatsappToken(), profile.getWhatsappBusinessAccountId())
				: templateSetupService.templateStatus(profile.getWhatsappToken(), profile.getWhatsappBusinessAccountId());
		profile.setWhatsappInvoiceTemplateStatus(resolveInvoiceStatus(templates));
		profile.setWhatsappLastVerifiedAt(LocalDateTime.now());
		doctorProfileDao.save(profile);
		return WhatsAppSettingsSupport.publicViewFull(
				profile.getWhatsappPhoneNumberId(),
				profile.getWhatsappBusinessAccountId(),
				profile.getWhatsappToken(),
				profile.getWhatsappConnectionStatus(),
				profile.getWhatsappInvoiceTemplateStatus(),
				templates);
	}

	@Transactional
	public void updateInvoiceTemplateStatusForWaba(String wabaId, String templateName, String metaStatus) {
		if (wabaId == null || wabaId.isBlank()) {
			return;
		}
		if (templateName != null && !templateName.isBlank()
				&& !WhatsAppTemplateSetupService.INVOICE_RECEIPT.equalsIgnoreCase(templateName)) {
			return;
		}
		String normalized = WhatsAppConnectionStatuses.normalizeTemplateStatus(metaStatus);
		List<Clinic> clinics = clinicRepository.findByWhatsappBusinessAccountId(wabaId.trim());
		for (Clinic clinic : clinics) {
			clinic.setWhatsappInvoiceTemplateStatus(normalized);
			clinicRepository.save(clinic);
		}
		List<DoctorProfile> doctors = doctorProfileDao.findByWhatsappBusinessAccountId(wabaId.trim());
		for (DoctorProfile profile : doctors) {
			profile.setWhatsappInvoiceTemplateStatus(normalized);
			doctorProfileDao.save(profile);
		}
	}

	@SuppressWarnings("unchecked")
	private static String resolveInvoiceStatus(Map<String, Object> templates) {
		if (templates == null) {
			return WhatsAppConnectionStatuses.TEMPLATE_MISSING;
		}
		if (Boolean.TRUE.equals(templates.get("templatesReady"))) {
			return WhatsAppConnectionStatuses.TEMPLATE_APPROVED;
		}
		Object list = templates.get("templates");
		if (list instanceof List<?> rows) {
			for (Object row : rows) {
				if (row instanceof Map<?, ?> m
						&& WhatsAppTemplateSetupService.INVOICE_RECEIPT.equals(String.valueOf(m.get("name")))) {
					return WhatsAppConnectionStatuses.normalizeTemplateStatus(String.valueOf(m.get("status")));
				}
			}
		}
		Object overall = templates.get("templatesStatus");
		return WhatsAppConnectionStatuses.normalizeTemplateStatus(overall == null ? null : String.valueOf(overall));
	}
}
