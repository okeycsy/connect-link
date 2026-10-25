package com.connectlink.contact;

import com.connectlink.auth.CurrentUser;
import com.connectlink.contact.dto.ContactResponse;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationOverviewContributor;
import com.connectlink.organization.OrganizationOverviewSectionKey;
import com.connectlink.organization.dto.OrganizationOverviewItem;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** 기관 한눈에 보기의 "담당자" 칸. 그 기관의 그 업무 담당자 전부와 전화·이메일. 6개월 넘게 확인하지 않은 사람은 "확인 필요" 로 눈에 띄게 보인다. */
@Component
public class ContactOrganizationOverview implements OrganizationOverviewContributor {

  private final ContactService contactService;

  public ContactOrganizationOverview(ContactService contactService) {
    this.contactService = contactService;
  }

  @Override
  public OrganizationOverviewSectionKey key() {
    return OrganizationOverviewSectionKey.CONTACT;
  }

  @Override
  public List<OrganizationOverviewItem> contribute(
      CurrentUser user, Business business, Organization organization) {
    List<ContactResponse> contacts =
        contactService.ofOrganization(user, business, organization.getId());
    if (contacts == null) {
      return null;
    }
    return contacts.stream()
        .map(
            c ->
                new OrganizationOverviewItem(
                    c.jobTitle() == null ? c.name() : c.name() + " " + c.jobTitle(),
                    Stream.of(c.department(), c.team(), c.jobRoleLabel(), c.subBusiness())
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(" · ")),
                    c.needsConfirm() ? "확인 필요" : c.rankLabel(),
                    c.needsConfirm(),
                    "/" + business.getSlug() + "/contacts?id=" + c.id(),
                    c.extensionPhone(),
                    c.mobilePhone(),
                    c.email()))
        .toList();
  }
}
