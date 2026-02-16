module bayyinah.core {
  // You MUST explicitly export the packages you want the client to see
  exports com.ks.bayyinah.core.dto;
  exports com.ks.bayyinah.core.exception;
  exports com.ks.bayyinah.core.model;
  exports com.ks.bayyinah.core.query;
  exports com.ks.bayyinah.core.repository;

  // If you use reflection (like Jackson/JSON or Hibernate), add 'opens'
  // opens com.ks.bayyinah.core.models to com.fasterxml.jackson.databind;
}
