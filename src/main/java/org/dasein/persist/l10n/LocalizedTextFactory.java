package org.dasein.persist.l10n;

import org.dasein.persist.DaseinPersist;
import org.dasein.persist.PersistenceException;
import org.dasein.persist.PersistentCache;

import java.util.Collection;
import java.util.Locale;

/**
 * This is a concrete class for wrapping up the static functionality of LocalizedText. That can probably
 * all be moved into here at a later point.
 *
 * //TODO: Move static functions from LocalizedText to here.
 * @author Tom Howe
 */
public class LocalizedTextFactory {
    private final DaseinPersist persistence;
    private final PersistentCache<LocalizedText> factory;

    public LocalizedTextFactory(DaseinPersist persistence) throws PersistenceException{
        this.persistence = persistence;
        this.factory = persistence.getCache(LocalizedText.class, "localizationCode");
    }

    public void addTranslation(String textGroup, String textCode, Locale forLocale, String translation) throws PersistenceException{
        LocalizedText.addTranslation(this.persistence, this.factory, textGroup, textCode, forLocale, translation);
    }

    public Collection<LocalizedText> getTranslations(String textGroup, String textCode) throws PersistenceException {
        return LocalizedText.getTranslations(this.persistence, this.factory, textGroup, textCode);
    }
}
