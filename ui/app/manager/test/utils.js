export const getAppUrl = (baseUrl, realm) => {
  const appUrl = baseUrl + "manager/?realm=";
  return appUrl + realm;
};
