package com.kittyp.notification.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.entity.MessageRateCard;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.model.MessagingDtos.RateCardModel;
import com.kittyp.notification.repository.MessageRateCardRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessagingRateCardService implements ApplicationRunner {
    private final MessageRateCardRepository repository;

    @Override @Transactional
    public void run(ApplicationArguments args) {
        seed(MessageChannel.SMS, "0.35", 0, 160, 1000, "350.00");
        seed(MessageChannel.WHATSAPP, "0.25", 0, 1, 1000, "250.00");
        seed(MessageChannel.EMAIL, "0.05", 2000, 1, 1000, "50.00");
        repository.findByChannel(MessageChannel.EMAIL).ifPresent(card -> {
            if (card.getIncludedMonthly() == 10000) {
                card.setIncludedMonthly(2000);
                repository.save(card);
            }
        });
    }

    private void seed(MessageChannel channel,String rate,int included,int segment,int credits,String packPrice) {
        if (repository.findByChannel(channel).isPresent()) return;
        MessageRateCard card = new MessageRateCard();
        card.setChannel(channel); card.setUnitRate(new BigDecimal(rate)); card.setIncludedMonthly(included);
        card.setSegmentSize(segment); card.setPackCredits(credits); card.setPackPriceExTax(new BigDecimal(packPrice));
        card.setTaxPercent(new BigDecimal("18.000"));
        repository.save(card);
    }

    @Transactional(readOnly=true)
    public MessageRateCard get(MessageChannel channel) {
        return repository.findByChannel(channel).orElseThrow(() ->
                new CustomException("Pricing is not configured for " + channel, HttpStatus.SERVICE_UNAVAILABLE));
    }
    @Transactional(readOnly=true)
    public List<RateCardModel> all() {
        List<RateCardModel> result=new ArrayList<>();
        for(MessageRateCard card:repository.findAll()) result.add(model(card));
        return result;
    }
    @Transactional
    public List<RateCardModel> replace(List<RateCardModel> updates) {
        if(updates.size()!=MessageChannel.values().length) throw new CustomException(
                "Provide exactly one rate card for SMS, WHATSAPP and EMAIL", HttpStatus.BAD_REQUEST);
        java.util.Set<MessageChannel> seen=new java.util.HashSet<>();
        for(RateCardModel update:updates) {
            if(!seen.add(update.channel()) || update.unitRate()==null || update.unitRate().signum()<0
                    || update.includedMonthly()<0 || update.segmentSize()<1 || update.packCredits()<1
                    || update.packPriceExTax()==null || update.packPriceExTax().signum()<0
                    || update.taxPercent()==null || update.taxPercent().signum()<0
                    || update.taxPercent().compareTo(new BigDecimal("100"))>0) {
                throw new CustomException("Invalid messaging rate card", HttpStatus.BAD_REQUEST);
            }
        }
        if(seen.size()!=MessageChannel.values().length) throw new CustomException(
                "Provide one rate card for each channel", HttpStatus.BAD_REQUEST);
        List<RateCardModel> result=new ArrayList<>();
        for(RateCardModel update:updates) {
            MessageRateCard card=repository.findByChannel(update.channel()).orElseGet(MessageRateCard::new);
            card.setChannel(update.channel()); card.setUnitRate(update.unitRate());
            card.setIncludedMonthly(update.includedMonthly()); card.setSegmentSize(update.segmentSize());
            card.setPackCredits(update.packCredits()); card.setPackPriceExTax(update.packPriceExTax());
            card.setTaxPercent(update.taxPercent());
            result.add(model(repository.save(card)));
        }
        return result;
    }
    private RateCardModel model(MessageRateCard c) {
        return new RateCardModel(c.getChannel(),c.getUnitRate(),c.getIncludedMonthly(),c.getSegmentSize(),
                c.getPackCredits(),c.getPackPriceExTax(),c.getTaxPercent());
    }
}
