package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.ai.service.MatchImportance;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.tactics.service.TacticalMatchModifiers;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class MatchModifierService {

    private final Random random = new Random();

    public MatchContext createContext(MatchImportance importance) {
        MatchContext context = new MatchContext(selectWeather());
        if (importance.extraTimePossible()) {
            context.setMatchIntensity(1.08);
        }
        return context;
    }

    public TacticalMatchModifiers applyContext(TacticalMatchModifiers tactics,
                                               MatchContext context,
                                               boolean homeTeam) {
        double momentum = homeTeam ? context.getHomeMomentum() : context.getAwayMomentum();
        double pressure = homeTeam ? context.getHomePressure() : context.getAwayPressure();
        double intent = homeTeam ? context.getHomeAttackingIntent() : context.getAwayAttackingIntent();
        double confidence = homeTeam
                ? context.getHomeDefensiveConfidence()
                : context.getAwayDefensiveConfidence();

        double weatherPassing = switch (context.getWeather()) {
            case RAIN -> -0.14;
            case SNOW -> -0.2;
            default -> 0.0;
        };
        double weatherFatigue = switch (context.getWeather()) {
            case HOT -> 0.22;
            case SNOW -> 0.12;
            case RAIN -> 0.08;
            default -> 0.0;
        };
        double weatherPress = switch (context.getWeather()) {
            case HOT -> -0.16;
            case SNOW -> -0.1;
            default -> 0.0;
        };
        double extraTimeTempo = context.isExtraTime() ? -0.18 : 0.0;

        return new TacticalMatchModifiers(
                tactics.possessionModifier() + momentum * 0.08 - pressure * 0.03 + weatherPassing,
                tactics.attackModifier() + momentum * 0.12 + pressure * 0.08 + (intent - 1.0) * 0.2,
                tactics.defenseModifier() + (confidence - 1.0) * 0.18 - pressure * 0.04,
                tactics.counterModifier() + pressure * 0.06,
                tactics.pressModifier() + pressure * 0.1 + weatherPress + extraTimeTempo,
                tactics.fatigueModifier() + weatherFatigue + pressure * 0.05
                        + Math.max(0, tactics.pressModifier()) * 0.12
                        + (context.isExtraTime() ? 0.35 : 0.0),
                tactics.disciplineModifier() + pressure * 0.04 + weatherFatigue * 0.35,
                tactics.passingModifier() + momentum * 0.07 + weatherPassing
                        - (context.isExtraTime() ? 0.08 : 0.0),
                tactics.crossingModifier() + pressure * 0.08,
                tactics.offsideModifier()
        );
    }

    public void updateForMinute(MatchContext context,
                                int minute,
                                int homeGoals,
                                int awayGoals,
                                MatchImportance importance) {
        boolean extraTime = importance.extraTimePossible() && minute > 90;
        context.setExtraTime(extraTime);
        context.setCurrentPhase(MatchPhase.fromMinute(minute, extraTime));
        decay(context);
        applyScoreState(context, homeGoals, awayGoals);
        applyPhase(context, importance);
    }

    public void applyEvent(MatchContext context,
                           MatchEventResponse event,
                           boolean homeEvent,
                           int homeGoals,
                           int awayGoals,
                           MatchImportance importance) {
        updateForMinute(context, event.minute(), homeGoals, awayGoals, importance);
        MatchEventType type = MatchEventType.valueOf(event.eventType());
        if (type == MatchEventType.GOAL || type == MatchEventType.PENALTY) {
            momentumSwing(context, homeEvent, 1.25);
            adjustDefense(context, homeEvent, -0.12);
            resetAttacks(context, homeEvent);
        } else if (type == MatchEventType.ASSIST) {
            momentumSwing(context, homeEvent, 0.35);
        } else if (type == MatchEventType.RED_CARD) {
            redCard(context, homeEvent);
        } else if (type == MatchEventType.YELLOW_CARD) {
            bookedTeam(context, homeEvent);
        } else if (type == MatchEventType.OWN_GOAL) {
            momentumSwing(context, !homeEvent, 0.9);
            adjustDefense(context, homeEvent, -0.18);
        } else if (type == MatchEventType.SUBSTITUTION) {
            freshLegs(context, homeEvent);
        }
    }

    public int goalMinute(boolean homeTeam,
                          int homeGoals,
                          int awayGoals,
                          MatchContext context,
                          MatchImportance importance) {
        int minimum = context.isExtraTime() ? 91 : 1;
        int maximum = context.isExtraTime() ? 120 : 90;
        int minute = random.nextInt(maximum - minimum + 1) + minimum;
        double pressure = homeTeam ? context.getHomePressure() : context.getAwayPressure();
        boolean trailing = homeTeam ? homeGoals < awayGoals : awayGoals < homeGoals;
        if ((trailing || pressure > 1.1) && random.nextInt(100) < 35) {
            minute = random.nextInt(maximum - Math.max(61, minimum) + 1) + Math.max(61, minimum);
        }
        updateForMinute(context, minute, homeGoals, awayGoals, importance);
        return minute;
    }

    public int probabilityAdjustment(MatchContext context, boolean homeTeam) {
        double pressure = homeTeam ? context.getHomePressure() : context.getAwayPressure();
        double momentum = homeTeam ? context.getHomeMomentum() : context.getAwayMomentum();
        double weatherMistake = switch (context.getWeather()) {
            case RAIN -> 5;
            case SNOW -> 4;
            default -> 0;
        };
        return (int) Math.round(momentum * 4 + pressure * 3 + weatherMistake);
    }

    public double shootoutScore(boolean homeTeam,
                                double shooterConfidence,
                                double goalkeeperConfidence,
                                double fatigue,
                                int kickNumber,
                                MatchContext context) {
        double homeCalm = homeTeam ? 1.5 : 0.0;
        double pressure = kickNumber >= 5 ? 4.0 : kickNumber * 0.45;
        double weatherPenalty = context.getWeather() == WeatherCondition.SNOW ? 3.0
                : context.getWeather() == WeatherCondition.RAIN ? 2.0 : 0.0;
        return 72 + homeCalm + shooterConfidence * 0.12 - goalkeeperConfidence * 0.08
                - fatigue * 0.08 - pressure - weatherPenalty;
    }

    private WeatherCondition selectWeather() {
        int roll = random.nextInt(100);
        if (roll < 70) {
            return WeatherCondition.CLEAR;
        }
        if (roll < 84) {
            return WeatherCondition.RAIN;
        }
        if (roll < 94) {
            return WeatherCondition.HOT;
        }
        return WeatherCondition.SNOW;
    }

    private void decay(MatchContext context) {
        context.setHomeMomentum(clamp(context.getHomeMomentum() * 0.92, -2.0, 2.0));
        context.setAwayMomentum(clamp(context.getAwayMomentum() * 0.92, -2.0, 2.0));
        context.setHomePressure(clamp(context.getHomePressure() * 0.96, 0.0, 2.2));
        context.setAwayPressure(clamp(context.getAwayPressure() * 0.96, 0.0, 2.2));
    }

    private void applyScoreState(MatchContext context, int homeGoals, int awayGoals) {
        if (homeGoals > awayGoals) {
            context.setHomeAttackingIntent(0.82);
            context.setAwayAttackingIntent(1.2);
            context.setAwayPressure(clamp(context.getAwayPressure() + 0.25, 0, 2.2));
        } else if (awayGoals > homeGoals) {
            context.setHomeAttackingIntent(1.2);
            context.setAwayAttackingIntent(0.82);
            context.setHomePressure(clamp(context.getHomePressure() + 0.25, 0, 2.2));
        } else {
            context.setHomeAttackingIntent(1.0);
            context.setAwayAttackingIntent(1.0);
        }
    }

    private void applyPhase(MatchContext context, MatchImportance importance) {
        if (context.getCurrentPhase() == MatchPhase.CLOSING && importance.extraTimePossible()) {
            context.setHomePressure(clamp(context.getHomePressure() + 0.25, 0, 2.2));
            context.setAwayPressure(clamp(context.getAwayPressure() + 0.25, 0, 2.2));
        }
        if (context.getCurrentPhase() == MatchPhase.EXTRA_TIME) {
            context.setMatchIntensity(0.86);
            context.setHomeAttackingIntent(context.getHomeAttackingIntent() * 0.92);
            context.setAwayAttackingIntent(context.getAwayAttackingIntent() * 0.92);
        }
    }

    private void momentumSwing(MatchContext context, boolean homeEvent, double amount) {
        if (homeEvent) {
            context.setHomeMomentum(clamp(context.getHomeMomentum() + amount, -2, 2));
            context.setAwayMomentum(clamp(context.getAwayMomentum() - amount * 0.45, -2, 2));
            context.setHomeConsecutiveAttacks(context.getHomeConsecutiveAttacks() + 1);
            context.setAwayConsecutiveAttacks(0);
        } else {
            context.setAwayMomentum(clamp(context.getAwayMomentum() + amount, -2, 2));
            context.setHomeMomentum(clamp(context.getHomeMomentum() - amount * 0.45, -2, 2));
            context.setAwayConsecutiveAttacks(context.getAwayConsecutiveAttacks() + 1);
            context.setHomeConsecutiveAttacks(0);
        }
    }

    private void redCard(MatchContext context, boolean homeEvent) {
        if (homeEvent) {
            context.setHomePressure(clamp(context.getHomePressure() + 0.6, 0, 2.2));
            context.setHomeDefensiveConfidence(clamp(context.getHomeDefensiveConfidence() - 0.25, 0.4, 1.4));
            context.setAwayMomentum(clamp(context.getAwayMomentum() + 0.7, -2, 2));
        } else {
            context.setAwayPressure(clamp(context.getAwayPressure() + 0.6, 0, 2.2));
            context.setAwayDefensiveConfidence(clamp(context.getAwayDefensiveConfidence() - 0.25, 0.4, 1.4));
            context.setHomeMomentum(clamp(context.getHomeMomentum() + 0.7, -2, 2));
        }
    }

    private void bookedTeam(MatchContext context, boolean homeEvent) {
        if (homeEvent) {
            context.setHomePressure(clamp(context.getHomePressure() - 0.08, 0, 2.2));
        } else {
            context.setAwayPressure(clamp(context.getAwayPressure() - 0.08, 0, 2.2));
        }
    }

    private void freshLegs(MatchContext context, boolean homeEvent) {
        if (homeEvent) {
            context.setHomePressure(clamp(context.getHomePressure() - 0.12, 0, 2.2));
        } else {
            context.setAwayPressure(clamp(context.getAwayPressure() - 0.12, 0, 2.2));
        }
    }

    private void resetAttacks(MatchContext context, boolean homeEvent) {
        if (homeEvent) {
            context.setHomeConsecutiveAttacks(0);
        } else {
            context.setAwayConsecutiveAttacks(0);
        }
    }

    private void adjustDefense(MatchContext context, boolean attackingHome, double amount) {
        if (attackingHome) {
            context.setAwayDefensiveConfidence(clamp(context.getAwayDefensiveConfidence() + amount, 0.4, 1.4));
        } else {
            context.setHomeDefensiveConfidence(clamp(context.getHomeDefensiveConfidence() + amount, 0.4, 1.4));
        }
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
