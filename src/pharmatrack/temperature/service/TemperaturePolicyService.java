package pharmatrack.temperature.service;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.common.util.InputValidator;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.TemperaturePolicy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing pharmaceutical temperature policies.
 */
public class TemperaturePolicyService {
    private final InMemoryDataStore dataStore;

    public TemperaturePolicyService(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public TemperaturePolicy registerPolicy(String policyId, String name, double minTemp, double maxTemp,
                                            String unit, String version, String description) {
        if (policyId == null || policyId.trim().isEmpty()) {
            policyId = IdGenerator.nextId("POL");
        }
        if (dataStore.getPolicies().containsKey(policyId)) {
            throw new ValidationException("Policy with ID " + policyId + " already exists.");
        }
        InputValidator.requireValidTemperatureRange(minTemp, maxTemp);
        TemperaturePolicy policy = new TemperaturePolicy(policyId, name, minTemp, maxTemp, unit, version, LocalDate.now(), description, true);
        dataStore.getPolicies().put(policyId, policy);
        return policy;
    }

    public Optional<TemperaturePolicy> findPolicyById(String policyId) {
        if (policyId == null) return Optional.empty();
        return Optional.ofNullable(dataStore.getPolicies().get(policyId));
    }

    public List<TemperaturePolicy> getAllPolicies() {
        return new ArrayList<>(dataStore.getPolicies().values());
    }

    public TemperaturePolicy updatePolicy(String policyId, String name, double minTemp, double maxTemp, String description) {
        TemperaturePolicy policy = findPolicyById(policyId)
                .orElseThrow(() -> new ValidationException("Policy with ID " + policyId + " not found."));

        InputValidator.requireValidTemperatureRange(minTemp, maxTemp);
        policy.setName(name);
        policy.updateTemperatureLimits(minTemp, maxTemp);
        policy.setDescription(description);
        // Increment version to maintain audit trail
        try {
            double currentVer = Double.parseDouble(policy.getVersion());
            policy.setVersion(String.format("%.1f", currentVer + 0.1));
        } catch (NumberFormatException e) {
            policy.setVersion(policy.getVersion() + ".1");
        }
        policy.setEffectiveDate(LocalDate.now());
        return policy;
    }

    public void deactivatePolicy(String policyId) {
        TemperaturePolicy policy = findPolicyById(policyId)
                .orElseThrow(() -> new ValidationException("Policy with ID " + policyId + " not found."));
        policy.setActive(false);
    }
}
