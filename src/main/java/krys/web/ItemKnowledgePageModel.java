package krys.web;

import krys.itemknowledge.ItemKnowledgeSnapshot;

import java.util.List;

/** Model SSR dla zarządzania bazą wiedzy o itemach. */
public final class ItemKnowledgePageModel {
    private final ItemKnowledgeSnapshot snapshot;
    private final List<String> messages;
    private final List<String> errors;

    public ItemKnowledgePageModel(ItemKnowledgeSnapshot snapshot, List<String> messages, List<String> errors) {
        this.snapshot = snapshot;
        this.messages = messages == null ? List.of() : List.copyOf(messages);
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public ItemKnowledgeSnapshot getSnapshot() {
        return snapshot;
    }

    public List<String> getMessages() {
        return messages;
    }

    public List<String> getErrors() {
        return errors;
    }
}
