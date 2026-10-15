import { Table, Text } from '@mantine/core';

interface Props {
  colSpan: number;
  message: string;
}

/** 표에 보여 줄 행이 없을 때의 한 줄. */
export function TableEmpty({ colSpan, message }: Props) {
  return (
    <Table.Tr>
      <Table.Td colSpan={colSpan}>
        <Text c="dimmed" ta="center" py="lg">
          {message}
        </Text>
      </Table.Td>
    </Table.Tr>
  );
}
