package server;

import java.util.concurrent.ConcurrentHashMap;

public class KeyValueStore {
  private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
  
  /**
   * Inserts or updates a key-value pair into the store.
   * If the store previously contained a mapping for the key, the old value is replaced.
   *
   * @param key The key with which the specified value is to be associated.
   * @param value The value to be associated with the specified key.
   * @return the insertion/update status.
   */
  public String put(String key, String value){
    ServerLogger.log(new Response(true, "PUT", "[key]" + key + " added/updated").toString());
    return store.put(key, value);
  }

  /**
   * Retrieves the value to which the specified key is mapped, or returns {@code null}
   * if this store contains no mapping for the key.
   *
   * @param key The key whose associated value is to be returned.
   * @return The value to which the specified key is mapped, or {@code null} if this store
   *         contains no mapping for the key.
   */
  public String get(String key) {
    return store.get(key);
  }

  /**
   * Removes the mapping for a key from this store if it is present.
   * Returns the value to which this store previously associated the key, or
   * {@code null} if the store contained no mapping for t1he key.
   *
   * @param key The key whose mapping is to be removed from the store.
   * @return The previous value associated with {@code key}, or
   *         {@code null} if there was no mapping for {@code key}.
   */
  public String delete(String key) {
    
    String value = store.remove(key);
    Response res = (value != null)
      ? new Response(true, "DELETE", "[key]" + key + " deleted")
      : new Response(false, "DELETE", "[key]" + key +" not found");
    ServerLogger.log(res.toString());
    return value;
  }
}
