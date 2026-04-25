package krys.itemknowledge;

/** Repozytorium osobnej bazy wiedzy o itemach. */
public interface ItemKnowledgeRepository {
    ItemKnowledgeSnapshot load();

    void save(ItemKnowledgeSnapshot snapshot);
}
