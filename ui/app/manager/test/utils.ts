export const getAppUrl = (baseUrl: string, realm: string) => {
  const appUrl = baseUrl + "manager/?realm=";
  return appUrl + realm;
};
