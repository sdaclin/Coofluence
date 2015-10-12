package coofluence.model;

import java.time.LocalDateTime;

public class ESConfig {
    private LocalDateTime lastChangeDate;

    public LocalDateTime getLastChangeDate() {
        return lastChangeDate;
    }

    public void setLastChangeDate(LocalDateTime lastChangeDate) {
        this.lastChangeDate = lastChangeDate;
    }
}
