package com.cda.carpooling.integration;

import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.Trip;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service d'envoi d'emails via Mailjet.
 * Gère l'envoi asynchrone de notifications transactionnelles.
 */
@Service
@Slf4j
public class EmailService {

    private final MailjetClient mailjetClient;
    private final String fromEmail;
    private final String fromName;

    public EmailService(
            @Value("${mailjet.api-key}") String apiKey,
            @Value("${mailjet.secret-key}") String secretKey,
            @Value("${mailjet.from-email}") String fromEmail,
            @Value("${mailjet.from-name}") String fromName) {

        this.fromEmail = fromEmail;
        this.fromName = fromName;

        ClientOptions options = ClientOptions.builder()
                .apiKey(apiKey)
                .apiSecretKey(secretKey)
                .build();

        this.mailjetClient = new MailjetClient(options);

        log.info("EmailService initialisé");
    }

    /**
     * Envoie un email de notification de modification de trajet à tous les passagers.
     *
     * @param trip Trajet modifié
     * @param passengers Liste des passagers à notifier
     */
    @Async
    public void sendTripUpdateNotification(Trip trip, List<Person> passengers) {
        if (passengers.isEmpty()) {
            log.debug("Aucun passager à notifier pour tripId={}", trip.getId());
            return;
        }

        log.info("Envoi notification modification trajet {} à {} passagers",
                trip.getId(), passengers.size());

        passengers.forEach(passenger -> {
            if (passenger.getProfile() == null || passenger.getProfile().getFirstname() == null) {
                log.warn("Profil incomplet pour personne {} — email non envoyé", passenger.getId());
                return;
            }

            try {
                sendEmail(
                        passenger.getEmail(),
                        "Modification de votre trajet",
                        buildTripUpdateTemplate(trip, passenger)
                );
                log.debug("Email modification envoyé à {}", passenger.getEmail());
            } catch (Exception e) {
                log.error("Erreur envoi email à {} : {}", passenger.getEmail(), e.getMessage());
            }
        });
    }

    /**
     * Envoie un email de notification d'annulation de trajet à tous les passagers.
     *
     * @param trip Trajet annulé/supprimé
     * @param passengers Liste des passagers à notifier
     */
    @Async
    public void sendTripCancellationNotification(Trip trip, List<Person> passengers) {
        if (passengers.isEmpty()) {
            log.debug("Aucun passager à notifier pour tripId={}", trip.getId());
            return;
        }

        log.info("Envoi notification annulation trajet {} à {} passagers",
                trip.getId(), passengers.size());

        passengers.forEach(passenger -> {
            if (passenger.getProfile() == null || passenger.getProfile().getFirstname() == null) {
                log.warn("Profil incomplet pour personne {} — email non envoyé", passenger.getId());
                return;
            }

            try {
                sendEmail(
                        passenger.getEmail(),
                        "Annulation de votre trajet",
                        buildTripCancellationTemplate(trip, passenger)
                );
                log.debug("Email annulation envoyé à {}", passenger.getEmail());
            } catch (Exception e) {
                log.error("Erreur envoi email à {} : {}", passenger.getEmail(), e.getMessage());
            }
        });
    }

    /**
     * Envoie un email de réinitialisation de mot de passe.
     *
     * @param person Personne qui a demandé la réinitialisation
     * @param resetToken Token de réinitialisation (en clair, non hashé)
     * @param validityMinutes Durée de validité du token en minutes
     */
    @Async
    public void sendPasswordResetEmail(Person person, String resetToken, int validityMinutes) {
        log.info("📧 Envoi email de réinitialisation à {}", person.getEmail());

        try {
            sendEmail(
                    person.getEmail(),
                    "Réinitialisation de votre mot de passe",
                    buildPasswordResetTemplate(person, resetToken, validityMinutes)
            );
            log.debug("✅ Email de réinitialisation envoyé à {}", person.getEmail());
        } catch (Exception e) {
            log.error("❌ Erreur envoi email de réinitialisation à {} : {}",
                    person.getEmail(), e.getMessage());
        }
    }

    /**
     * Envoie un email via Mailjet.
     */
    private void sendEmail(String toEmail, String subject, String htmlContent) throws MailjetException {
        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", fromEmail)
                                        .put("Name", fromName))
                                .put(Emailv31.Message.TO, new JSONArray()
                                        .put(new JSONObject()
                                                .put("Email", toEmail)))
                                .put(Emailv31.Message.SUBJECT, subject)
                                .put(Emailv31.Message.HTMLPART, htmlContent)));

        MailjetResponse response = mailjetClient.post(request);

        if (response.getStatus() != 200) {
            throw new MailjetException("Erreur Mailjet: " + response.getData());
        }
    }

    //region Templates HTML
    /**
     * Template HTML pour notification de modification de trajet.
     */
    private String buildTripUpdateTemplate(Trip trip, Person passenger) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
                        .trip-info { background: white; padding: 20px; margin: 20px 0; border-left: 4px solid #4CAF50; }
                        .trip-info p { margin: 10px 0; }
                        .label { font-weight: bold; color: #666; }
                        .footer { text-align: center; margin-top: 30px; color: #999; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Modification de trajet</h1>
                        </div>
                        <div class="content">
                            <p>Bonjour <strong>%s</strong>,</p>
                            
                            <p>Le trajet suivant a été modifié par le conducteur :</p>
                            
                            <div class="trip-info">
                                <p><span class="label">Départ :</span> %s</p>
                                <p><span class="label">Arrivée :</span> %s</p>
                                <p><span class="label">Date :</span> %s</p>
                                <p><span class="label">Distance :</span> %s km</p>
                                <p><span class="label">Durée :</span> %s min</p>
                                <p><span class="label">Places disponibles :</span> %d</p>
                            </div>
                            
                            <p>Votre réservation reste valide. Si ces modifications ne vous conviennent pas, vous pouvez annuler votre réservation depuis votre compte.</p>
                            
                            <p>Bon voyage ! 🚗</p>
                        </div>
                        <div class="footer">
                            <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                passenger.getProfile().getFirstname(),
                formatAddress(trip.getDepartureAddress()),
                formatAddress(trip.getArrivingAddress()),
                trip.getTripDatetime().format(dateFormatter),
                trip.getDistanceKm() != null ? String.format("%.1f", trip.getDistanceKm()) : "N/A",
                trip.getDurationMinutes() != null ? trip.getDurationMinutes().toString() : "N/A",
                trip.getAvailableSeats()
        );
    }

    /**
     * Template HTML pour notification d'annulation de trajet.
     */
    private String buildTripCancellationTemplate(Trip trip, Person passenger) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #f44336; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
                        .trip-info { background: white; padding: 20px; margin: 20px 0; border-left: 4px solid #f44336; }
                        .trip-info p { margin: 10px 0; }
                        .label { font-weight: bold; color: #666; }
                        .footer { text-align: center; margin-top: 30px; color: #999; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Annulation de trajet</h1>
                        </div>
                        <div class="content">
                            <p>Bonjour <strong>%s</strong>,</p>
                            
                            <p>Nous vous informons que le trajet suivant a été <strong>annulé</strong> par le conducteur :</p>
                            
                            <div class="trip-info">
                                <p><span class="label">Départ :</span> %s</p>
                                <p><span class="label">Arrivée :</span> %s</p>
                                <p><span class="label">Date :</span> %s</p>
                            </div>
                            
                            <p>Votre réservation a été automatiquement annulée. Nous vous invitons à consulter nos autres trajets disponibles.</p>
                            
                            <p>Nous nous excusons pour ce désagrément.</p>
                        </div>
                        <div class="footer">
                            <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                passenger.getProfile().getFirstname(),
                formatAddress(trip.getDepartureAddress()),
                formatAddress(trip.getArrivingAddress()),
                trip.getTripDatetime().format(dateFormatter)
        );
    }

    /**
     * Template HTML pour email de réinitialisation de mot de passe.
     */
    private String buildPasswordResetTemplate(Person person, String resetToken, int validityMinutes) {
        String resetUrl = String.format("%s/auth/reset-password?token=%s",
                System.getenv().getOrDefault("FRONTEND_URL", "http://localhost:3000"),
                resetToken);

        String greeting = (person.getProfile() != null && person.getProfile().getFirstname() != null)
                ? person.getProfile().getFirstname()
                : person.getEmail();

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background: #2196F3; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
                .button-container { text-align: center; margin: 30px 0; }
                .reset-button {
                    display: inline-block;
                    padding: 15px 30px;
                    background: #2196F3;
                    color: white !important;
                    text-decoration: none;
                    border-radius: 5px;
                    font-weight: bold;
                }
                .warning-box {
                    background: #fff3cd;
                    border-left: 4px solid #ffc107;
                    padding: 15px;
                    margin: 20px 0;
                }
                .danger-box {
                    background: #ffebee;
                    border-left: 4px solid #f44336;
                    padding: 15px;
                    margin: 20px 0;
                }
                .footer { text-align: center; margin-top: 30px; color: #999; font-size: 12px; }
                .expiry { color: #f44336; font-weight: bold; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🔐 Réinitialisation de mot de passe</h1>
                </div>
                <div class="content">
                    <p>Bonjour <strong>%s</strong>,</p>
                    
                    <p>Vous avez demandé à réinitialiser votre mot de passe sur Carpooling.</p>
                    
                    <div class="button-container">
                        <a href="%s" class="reset-button">Réinitialiser mon mot de passe</a>
                    </div>
                    
                    <div class="warning-box">
                        <p><strong>⏱️ Ce lien est valide pendant <span class="expiry">%d minutes</span></strong></p>
                    </div>
                    
                    <div class="danger-box">
                        <p><strong>⚠️ Si vous n'avez pas demandé cette réinitialisation :</strong></p>
                        <ul>
                            <li>Ignorez simplement cet email</li>
                            <li>Votre mot de passe actuel reste inchangé</li>
                            <li>Le lien expirera automatiquement dans %d minutes</li>
                        </ul>
                    </div>
                    
                    <p style="margin-top: 30px; color: #666; font-size: 14px;">
                        Si le bouton ne fonctionne pas, copiez-collez ce lien dans votre navigateur :<br>
                        <a href="%s" style="color: #2196F3; word-break: break-all;">%s</a>
                    </p>
                </div>
                <div class="footer">
                    <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                    <p>© 2026 Carpooling - Tous droits réservés</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                greeting,
                resetUrl,
                validityMinutes,
                validityMinutes,
                resetUrl,
                resetUrl
        );
    }

    /**
     * Formate une adresse pour affichage dans les emails.
     */
    private String formatAddress(com.cda.carpooling.entity.Address address) {
        return String.format("%s %s, %s (%s)",
                address.getStreetNumber(),
                address.getStreetName(),
                address.getCity().getName(),
                address.getCity().getPostalCode());
    }
    //endregion
}